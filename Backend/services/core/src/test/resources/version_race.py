#!/usr/bin/env python3
"""
Lasttest fuer den Lost-Update im NodeDao.changeContent-Pfad:

    org.springframework.dao.ConcurrencyFailureException:
    Unexpected: current version does not appear to be 1st version in the list

Ablauf
  1. legt (oder nutzt via --node) einen ccm:io im -userhome- an
  2. feuert pro Runde N parallele POST .../content?versionComment=... (multipart)
     (gleicher Pfad wie der OnlyOffice-Connector), synchronisiert ueber eine Barrier
  3. nach jeder Runde ein einzelner, sequentieller Probe-Save:
     schlaegt der fehl, ist der Knoten "kaputt" (Label != Head der Versionshistorie)
  4. vergleicht cm:versionLabel des Knotens mit der hoechsten Version aus /versions

Erwartung
  9.1  (ohne f1e29397d4): frueher oder spaeter ein dauerhaft kaputter Knoten
  10.0 (mit Fix):          vereinzelt 5xx unter Last moeglich, aber Probe-Save immer OK

Nur stdlib, Python >= 3.8.
"""
import argparse
import base64
import csv
import json
import ssl
import statistics
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections import Counter

ERROR_MARKER = "current version does not appear"


class Client:
    def __init__(self, base, user, password, insecure):
        self.base = base.rstrip("/") + "/rest"
        token = base64.b64encode(f"{user}:{password}".encode()).decode()
        self.headers = {"Authorization": "Basic " + token, "Accept": "application/json"}
        self.ctx = ssl._create_unverified_context() if insecure else None
        self.padding = b""

    def request(self, method, path, params=None, body=None, content_type=None, timeout=120):
        url = self.base + path
        if params:
            url += "?" + urllib.parse.urlencode(params, doseq=True)
        headers = dict(self.headers)
        data = None
        if body is not None:
            data = body if isinstance(body, bytes) else body.encode()
            headers["Content-Type"] = content_type or "application/json"
        req = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=timeout, context=self.ctx) as r:
                return r.status, r.read().decode(errors="replace")
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode(errors="replace")
        except Exception as e:  # timeouts, connection resets
            return -1, repr(e)

    def create_node(self, name):
        status, body = self.request(
            "POST", "/node/v1/nodes/-home-/-userhome-/children",
            params={"type": "ccm:io", "renameIfExists": "true", "versionComment": "initial"},
            body=json.dumps({"cm:name": [name]}))
        if status != 200:
            sys.exit(f"Knoten anlegen fehlgeschlagen: {status} {body[:500]}")
        return json.loads(body)["node"]["ref"]["id"]

    def save(self, node_id, text, comment):
        # multipart /content statt /textContent: letzteres liefert auf 9.1 immer 415
        boundary = uuid.uuid4().hex
        # padding (--size) haengt an den eindeutigen Text an, damit jede Version anderen Inhalt hat
        body = (f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="file"; filename="loadtest.txt"\r\n'
                "Content-Type: text/plain\r\n\r\n"
                f"{text}").encode() + self.padding + f"\r\n--{boundary}--\r\n".encode()
        return self.request(
            "POST", f"/node/v1/nodes/-home-/{node_id}/content",
            params={"mimetype": "text/plain", "versionComment": comment},
            body=body, content_type=f"multipart/form-data; boundary={boundary}")

    def touch_metadata(self, node_id, value):
        # zweiter Schreiber: updateNode + createVersion ohne Content-Upload -> zaehlt das Label schneller hoch
        return self.request(
            "POST", f"/node/v1/nodes/-home-/{node_id}/metadata",
            params={"versionComment": f"meta {value}"},
            body=json.dumps({"cclom:general_description": [value]}))

    def version_label(self, node_id):
        status, body = self.request("GET", f"/node/v1/nodes/-home-/{node_id}/metadata",
                                    params={"propertyFilter": "-all-"})
        if status != 200:
            return None
        props = json.loads(body)["node"].get("properties", {})
        for key in ("cm:versionLabel", "{http://www.alfresco.org/model/content/1.0}versionLabel"):
            if props.get(key):
                return props[key][0]
        return None

    def head_version(self, node_id):
        status, body = self.request("GET", f"/node/v1/nodes/-home-/{node_id}/versions")
        if status != 200:
            return None, 0
        versions = json.loads(body).get("versions", [])
        if not versions:
            return None, 0
        # 9.1 liefert NodeVersionRef direkt, neuere Versionen NodeVersion mit "version"-Feld
        refs = [(v.get("version", v)["major"], v.get("version", v)["minor"]) for v in versions]
        top = max(refs)
        return f"{top[0]}.{top[1]}", len(refs)


def classify(status, body):
    if status == 200:
        return "ok"
    if ERROR_MARKER in body:
        return "VERSION_NOT_HEAD"
    if "ConcurrencyFailure" in body or "concurrency" in body.lower():
        return "concurrency"
    if status == -1:
        return "client_error"
    return f"http_{status}"


def run_round(client, node_id, rnd, threads, mix_metadata):
    barrier = threading.Barrier(threads)
    results = []
    lock = threading.Lock()

    def worker(i):
        try:
            barrier.wait(timeout=30)
        except threading.BrokenBarrierError:
            pass
        start = time.perf_counter()
        if mix_metadata and i % 2 == 1:
            status, body = client.touch_metadata(node_id, f"r{rnd}-t{i}-{time.time()}")
            kind = "meta:" + classify(status, body)
            op = "meta"
        else:
            status, body = client.save(node_id, f"round {rnd} thread {i} {time.time()}\n",
                                       f"loadtest r{rnd} t{i}")
            kind = classify(status, body)
            op = "save"
        elapsed = time.perf_counter() - start
        with lock:
            results.append((kind, status, body, op, elapsed))

    ts = [threading.Thread(target=worker, args=(i,)) for i in range(threads)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    return results


def parse_size(value):
    units = {"": 1, "B": 1, "KB": 1024, "MB": 1024 ** 2, "GB": 1024 ** 3}
    v = value.strip().upper()
    number = v.rstrip("KMGB")
    unit = v[len(number):]
    if unit not in units:
        raise argparse.ArgumentTypeError(f"unbekannte Einheit in {value!r}, erlaubt: B, KB, MB, GB")
    return int(float(number) * units[unit])


def make_padding(size):
    line = b"edu-sharing version race load test padding line 0123456789 abcdefghijklmnopqrstuvwxyz\n"
    return (line * (size // len(line) + 1))[:size]


def percentile(values, q):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(round(q * (len(ordered) - 1))))]


def print_timings(timings, wall):
    print(f"\nZeiten in Sekunden (Laufzeit gesamt {wall:.1f}s):")
    print(f"  {'Operation':<22} {'n':>5} {'mean':>7} {'median':>7} {'p95':>7} {'max':>7}")
    for op, values in timings.items():
        if not values:
            continue
        print(f"  {op:<22} {len(values):>5} {statistics.mean(values):>7.3f} "
              f"{statistics.median(values):>7.3f} {percentile(values, 0.95):>7.3f} {max(values):>7.3f}")
    saves = len(timings.get("save (parallel)", [])) + len(timings.get("save (probe, seriell)", []))
    if wall > 0:
        print(f"  Durchsatz Content-Saves: {saves / wall:.2f}/s")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--url", default="http://localhost:8080/edu-sharing")
    p.add_argument("--user", default="admin")
    p.add_argument("--password", default="admin")
    p.add_argument("--node", help="vorhandenen Knoten nutzen statt neu anzulegen")
    p.add_argument("--threads", type=int, default=8, help="parallele Saves pro Runde")
    p.add_argument("--rounds", type=int, default=200)
    p.add_argument("--pause", type=float, default=0.0, help="Sekunden zwischen Runden")
    p.add_argument("--mix-metadata", action="store_true",
                   help="jeder zweite Thread schreibt Metadaten mit neuer Version statt Content. "
                        "ACHTUNG: provoziert auf 9.1 einen Deadlock (synchronized createVersion + alf_node-Row-Lock)")
    p.add_argument("--keep-going", action="store_true",
                   help="nach kaputtem Knoten nicht abbrechen")
    p.add_argument("--insecure", action="store_true", help="TLS-Zertifikat nicht pruefen")
    p.add_argument("--csv", help="Einzelzeiten aller Requests als CSV schreiben")
    p.add_argument("--fresh-every", type=int, default=0, metavar="N",
                   help="alle N Runden einen neuen Knoten anlegen, damit die Versionshistorie klein bleibt")
    p.add_argument("--size", type=parse_size, default=0,
                   help="Groesse des Contents pro Save, z.B. 500KB, 5MB (Default: nur wenige Byte Text)")
    a = p.parse_args()

    c = Client(a.url, a.user, a.password, a.insecure)
    c.padding = make_padding(a.size)
    if a.size:
        print(f"Content-Groesse pro Save: {a.size / 1024 ** 2:.2f} MB")
    node_id = a.node or c.create_node(f"version-race-{int(time.time())}.txt")
    print(f"Knoten: {node_id}")
    st, body = c.save(node_id, "seed\n", "seed")
    if st != 200:
        sys.exit(f"Initialer Save fehlgeschlagen: {st} {body[:500]}")

    total = Counter()
    samples = {}
    timings = {"save (parallel)": [], "meta (parallel)": [], "save (probe, seriell)": []}
    rows = []
    wall_start = time.perf_counter()
    for rnd in range(1, a.rounds + 1):
        if a.fresh_every and rnd > 1 and (rnd - 1) % a.fresh_every == 0:
            node_id = c.create_node(f"version-race-{int(time.time())}.txt")
            st, body = c.save(node_id, "seed\n", "seed")
            if st != 200:
                sys.exit(f"Initialer Save fehlgeschlagen: {st} {body[:500]}")
            print(f"Neuer Knoten: {node_id}")
        results = run_round(c, node_id, rnd, a.threads, a.mix_metadata)
        rc = Counter(k for k, _, _, _, _ in results)
        total.update(rc)
        for kind, status, body, op, elapsed in results:
            if kind not in ("ok", "meta:ok") and kind not in samples:
                samples[kind] = f"{status} {body[:800]}"
            timings[f"{op} (parallel)"].append(elapsed)
            rows.append((rnd, op, status, kind, f"{elapsed:.4f}"))

        p_start = time.perf_counter()
        p_status, p_body = c.save(node_id, f"probe {rnd}\n", f"probe r{rnd}")
        p_elapsed = time.perf_counter() - p_start
        probe = classify(p_status, p_body)
        timings["save (probe, seriell)"].append(p_elapsed)
        rows.append((rnd, "probe", p_status, probe, f"{p_elapsed:.4f}"))
        round_saves = [e for _, _, _, op, e in results if op == "save"]
        label = c.version_label(node_id)
        head, count = c.head_version(node_id)
        mismatch = label is not None and head is not None and label != head

        print(f"Runde {rnd:4d}: {dict(rc)}  probe={probe}  label={label} head={head} "
              f"versions={count}  t_save avg={statistics.mean(round_saves):.2f}s "
              f"max={max(round_saves):.2f}s probe={p_elapsed:.2f}s"
              f"{'  <-- MISMATCH' if mismatch else ''}", flush=True)

        if probe != "ok" or mismatch:
            print("\n*** Knoten ist kaputt / inkonsistent ***")
            print(f"Probe-Save: {p_status} {p_body[:1500]}")
            if not a.keep_going:
                break
        if a.pause:
            time.sleep(a.pause)

    wall = time.perf_counter() - wall_start
    print("\nGesamt:", dict(total))
    print_timings(timings, wall)
    if a.csv:
        with open(a.csv, "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(["round", "op", "status", "kind", "seconds"])
            w.writerows(rows)
        print(f"  Einzelzeiten: {a.csv}")
    for kind, s in samples.items():
        print(f"\n--- Beispiel {kind} ---\n{s}")
    print(f"\nKnoten zum Nachsehen: {node_id}")
    print("SQL: select n.id, n.version, p.string_value from alf_node n "
          "join alf_node_properties p on p.node_id=n.id "
          "join alf_qname q on q.id=p.qname_id and q.local_name='versionLabel' "
          f"where n.uuid='{node_id}';")


if __name__ == "__main__":
    main()
