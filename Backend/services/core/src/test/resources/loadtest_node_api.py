#!/usr/bin/env python3
"""
Small load test against the edu-sharing node API.

Uploads go into an intermediate folder (default: "loadtest") below the given
parent node (default: -userhome-). The folder is created on the first run and
reused afterwards.

Per iteration the pipelines of all test files (default: pdf, docx, jpg and two
tiny text files) run in parallel, each in its own thread with its own HTTP
session, in order to provoke concurrency problems. Within a single node the
steps run sequentially:

  1. create ccm:io node in the target folder    POST   /rest/node/v1/nodes/{repo}/{folder}/children
  2. upload content (multipart)                 POST   /rest/node/v1/nodes/{repo}/{node}/content
  3. set metadata in SEPARATE calls             POST   /rest/node/v1/nodes/{repo}/{node}/metadata
                                                       ?versionComment=... (--no-versioning: PUT)
       a) ccm:educationallearningresourcetype
       b) cclom:general_keyword
       c) ccm:commonlicense_key + _cc_version
       d) cclom:general_description
     After every call getMetadata is used to check that the property is stored
     on the node. If it is missing only from the write call's response, a WARN
     line is printed (incomplete read-after-write) but the run continues.
  4. read everything back and check              GET   /rest/node/v1/nodes/{repo}/{node}/metadata
     (reveals updates lost by the read-modify-write in updateNodeNative)

Afterwards each iteration checks via the search whether all nodes made it into
the index (POST /rest/search/v1/queries/{repo}/-default-/ngsearch, searching for
the keyword that is unique per file). The tracker works asynchronously, so this
is polled (--search-interval) and only gives up after --search-timeout seconds.
Use --no-search to skip the step.

The search only answers whether the node is indexed. The metadata itself is
always checked through getMetadata of the node API - right after the write calls
and once more as soon as the node shows up in the index.

On the first failure (HTTP error, timeout, or missing/wrong metadata) the script
aborts immediately and leaves the nodes in place so they can be inspected
together with the logs.

Examples:
  ./loadtest_node_api.py -n 100
  ./loadtest_node_api.py -n 100 --concurrency 1          # fully sequential
  ./loadtest_node_api.py -n 100 --file-delay 0.2         # staggered start
  ./loadtest_node_api.py -n 100 --search-timeout 300
  ./loadtest_node_api.py -n 20 --parallel-metadata --no-search   # fastest repro
  ./loadtest_node_api.py -n 20 --file ~/Dokumente/edu-sharingTestFiles/pptx.pptx
"""

import argparse
import json
import mimetypes
import os
import sys
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

import requests

DEFAULT_BASE_URL = "http://repository.127.0.0.1.nip.io:8100/edu-sharing"

# default test files (deliberately a bit larger, from the local demo content)
DEFAULT_FILES = [
    "/home/mv/Dokumente/edu-demo-Inhalte/edu-sharing_vm_1.7_v1.pdf",          # ~216 KB
    "/home/mv/Dokumente/edu-sharingTestFiles/officedocs/pages.docx",          # ~307 KB
    "/home/mv/Dokumente/edu-sharingTestFiles/lake-65443_1920.jpg",            # ~948 KB
]

# additional tiny text files, generated in memory
DEFAULT_MINI_TXT = 2
DEFAULT_TXT_SIZE = 64

# mimetypes that mimetypes.guess_type does not know (reliably)
EXTRA_MIMETYPES = {
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ".pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    ".odt": "application/vnd.oasis.opendocument.text",
    ".ods": "application/vnd.oasis.opendocument.spreadsheet",
    ".odp": "application/vnd.oasis.opendocument.presentation",
    ".h5p": "application/zip",
}

# properties that are written and verified afterwards
PROP_NAME = "cm:name"
PROP_TITLE = "cclom:title"
PROP_LRT = "ccm:educationallearningresourcetype"
PROP_KEYWORD = "cclom:general_keyword"
PROP_LICENSE = "ccm:commonlicense_key"
PROP_LICENSE_VERSION = "ccm:commonlicense_cc_version"
PROP_DESCRIPTION = "cclom:general_description"

# valid key from valuespaces.xml for ccm:educationallearningresourcetype
LRT_VALUE = "exercise"
LICENSE_VALUE = "CC_BY"
LICENSE_VERSION_VALUE = "4.0"


class LoadTestError(Exception):
    """Reason for aborting, including everything needed for log analysis."""

    def __init__(self, step, message, response=None, node_id=None):
        super().__init__(message)
        self.step = step
        self.message = message
        self.response = response
        self.node_id = node_id


class TestFile:
    """A test file, either read from disk or generated in memory."""

    def __init__(self, basename, data, mimetype=None):
        self.basename = basename
        self.data = data
        self.stem, self.extension = os.path.splitext(basename)
        self.mimetype = mimetype or self.guess_mimetype(basename)

    @staticmethod
    def guess_mimetype(basename):
        extension = os.path.splitext(basename)[1].lower()
        return (EXTRA_MIMETYPES.get(extension)
                or mimetypes.guess_type(basename)[0]
                or "application/octet-stream")

    @classmethod
    def from_path(cls, path):
        full_path = os.path.abspath(os.path.expanduser(path))
        if not os.path.isfile(full_path):
            raise LoadTestError("init", "test file not found: %s" % full_path)
        with open(full_path, "rb") as handle:
            return cls(os.path.basename(full_path), handle.read())

    @classmethod
    def mini_txt(cls, number, size=DEFAULT_TXT_SIZE):
        """Tiny text file of exactly `size` bytes (the header is truncated if needed)."""
        size = max(1, size)
        head = "mini-%d edu-sharing Lasttest " % number
        data = (head + "x" * size)[:size]
        return cls("mini-%d.txt" % number, data.encode("ascii"), "text/plain")

    def __str__(self):
        size = len(self.data)
        readable = "%d B" % size if size < 1024 else "%.1f KB" % (size / 1024.0)
        return "%s (%s, %s)" % (self.basename, readable, self.mimetype)


def metadata_steps(run_id, iteration, keyword, test_file):
    """
    The metadata is deliberately written in separate calls.

    Every step sends only its own properties; the previous ones have to survive
    on the server side (updateNodeNative merges with what is already stored).
    """
    return [
        ("lrt", {PROP_LRT: [LRT_VALUE]}),
        ("keyword", {PROP_KEYWORD: [keyword, "lasttest"]}),
        ("license", {PROP_LICENSE: [LICENSE_VALUE],
                     PROP_LICENSE_VERSION: [LICENSE_VERSION_VALUE]}),
        ("description", {PROP_DESCRIPTION: [
            "Load test %s, iteration %03d, file %s" % (run_id, iteration, test_file.basename)]}),
    ]


# ------------------------------------------------------------------- REST client


class NodeApi:
    """
    REST client with one HTTP session per thread.

    The parallel pipelines should behave like independent clients (own
    connection, own JSESSIONID) - and requests.Session is not thread-safe.
    """

    def __init__(self, base_url, user, password, repository, timeout):
        root = base_url.rstrip("/")
        self.base = root + "/rest/node/v1/nodes/" + repository
        self.search_base = root + "/rest/search/v1/queries/" + repository
        self.timeout = timeout
        self.auth = (user, password)
        self._local = threading.local()

    @property
    def session(self):
        session = getattr(self._local, "session", None)
        if session is None:
            session = requests.Session()
            session.auth = self.auth
            session.headers.update({"Accept": "application/json"})
            self._local.session = session
        return session

    def _check(self, step, response, node_id=None):
        if response.status_code >= 400:
            raise LoadTestError(
                step,
                "HTTP %d on %s %s" % (response.status_code, response.request.method, response.url),
                response=response,
                node_id=node_id,
            )
        try:
            return response.json()
        except ValueError:
            raise LoadTestError(step, "response is not JSON", response=response, node_id=node_id)

    def find_folder(self, parent, name):
        url = "%s/%s/children" % (self.base, parent)
        params = {"filter": "folders", "maxItems": 1000, "propertyFilter": "cm:name"}
        data = self._check("findFolder", self.session.get(url, params=params, timeout=self.timeout))
        for node in data.get("nodes") or []:
            if node.get("name") == name:
                return node["ref"]["id"]
        return None

    def ensure_folder(self, parent, name):
        """Create the intermediate folder or reuse the existing one."""
        existing = self.find_folder(parent, name)
        if existing:
            return existing, False

        url = "%s/%s/children" % (self.base, parent)
        params = {"type": "cm:folder", "renameIfExists": "false", "obeyMds": "false"}
        response = self.session.post(url, params=params, json={PROP_NAME: [name]},
                                     timeout=self.timeout)
        if response.status_code >= 400:
            # e.g. created concurrently -> look it up again
            existing = self.find_folder(parent, name)
            if existing:
                return existing, False
        return self._check("createFolder", response)["node"]["ref"]["id"], True

    def list_children_ids(self, parent):
        url = "%s/%s/children" % (self.base, parent)
        params = {"maxItems": 1000, "propertyFilter": "cm:name"}
        data = self._check("listChildren",
                           self.session.get(url, params=params, timeout=self.timeout))
        return {node["ref"]["id"]: node.get("name") for node in data.get("nodes") or []}

    def create_child(self, parent, name, title, obey_mds):
        url = "%s/%s/children" % (self.base, parent)
        params = {
            "type": "ccm:io",
            "renameIfExists": "true",
            "obeyMds": str(bool(obey_mds)).lower(),
        }
        body = {PROP_NAME: [name], PROP_TITLE: [title]}
        response = self.session.post(url, params=params, json=body, timeout=self.timeout)
        return self._check("createChild", response)["node"]["ref"]["id"]

    def upload_content(self, node_id, filename, mimetype, payload):
        url = "%s/%s/content" % (self.base, node_id)
        params = {"mimetype": mimetype, "versionComment": "MAIN_FILE_UPLOAD"}
        files = {"file": (filename, payload, mimetype)}
        response = self.session.post(url, params=params, files=files, timeout=self.timeout)
        return self._check("changeContent", response, node_id)

    def set_metadata(self, node_id, properties, obey_mds, step="changeMetadata",
                     version_comment=None):
        """
        version_comment=None -> PUT  /metadata              (NodeDao.changeProperties)
        version_comment      -> POST /metadata?versionComment=...
                                (NodeDao.changePropertiesWithVersioning - opens its own
                                 transaction, which disables the retry inside
                                 updateNodeNative; this is the code path from DESP-851)
        """
        url = "%s/%s/metadata" % (self.base, node_id)
        params = {"obeyMds": str(bool(obey_mds)).lower()}
        if version_comment:
            params["versionComment"] = version_comment
            response = self.session.post(url, params=params, json=properties,
                                         timeout=self.timeout)
        else:
            response = self.session.put(url, params=params, json=properties,
                                        timeout=self.timeout)
        return self._check(step, response, node_id)

    def get_metadata(self, node_id):
        url = "%s/%s/metadata" % (self.base, node_id)
        params = {"propertyFilter": "-all-"}
        response = self.session.get(url, params=params, timeout=self.timeout)
        return self._check("getMetadata", response, node_id)

    def search_by_keyword(self, keyword, mds="-default-", query="ngsearch"):
        """Search via the MDS query that the user interface uses as well."""
        url = "%s/%s/%s" % (self.search_base, mds, query)
        params = {"maxItems": 50, "contentType": "FILES", "propertyFilter": "cm:name"}
        body = {"criteria": [{"property": PROP_KEYWORD, "values": [keyword]}]}
        response = self.session.post(url, params=params, json=body, timeout=self.timeout)
        return self._check("search", response)

    def delete(self, node_id):
        url = "%s/%s" % (self.base, node_id)
        response = self.session.delete(url, params={"recycle": "false"}, timeout=self.timeout)
        if response.status_code >= 400:
            print("  WARN: deleting %s failed (HTTP %d)"
                  % (node_id, response.status_code), file=sys.stderr)


def missing_properties(node, expected):
    """Returns {property: value read, or None} for everything that does not match."""
    properties = node.get("properties") or {}
    result = {}
    for key, wanted in expected.items():
        actual = properties.get(key)
        if actual is None:
            result[key] = None
        elif [value for value in wanted if value not in actual]:
            result[key] = actual
    return result


def describe_missing(missing, expected):
    return ", ".join(
        "%s (expected %s, read %s)" % (key, expected[key], "not present" if actual is None
                                       else actual)
        for key, actual in sorted(missing.items()))


def verify(node_id, node, expected, step="verify"):
    """Checks that the expected properties are really stored on the node."""
    missing = missing_properties(node, expected)
    if missing:
        raise LoadTestError(
            step,
            "Property %s" % describe_missing(missing, expected),
            node_id=node_id,
        )


def verify_content(node_id, node, test_file, step="verify(content)"):
    """Checks that the content really reached the node (and is not 0 bytes)."""
    expected = len(test_file.data)
    try:
        actual = int(node.get("size"))
    except (TypeError, ValueError):
        raise LoadTestError(
            step,
            "node has no usable size (size=%r), expected %d bytes from %s"
            % (node.get("size"), expected, test_file.basename),
            node_id=node_id,
        )
    if actual != expected:
        raise LoadTestError(
            step,
            "content size differs: %d bytes on the node, %d bytes uploaded (%s)"
            % (actual, expected, test_file.basename),
            node_id=node_id,
        )
    mimetype = node.get("mimetype")
    if mimetype and test_file.mimetype and mimetype != test_file.mimetype:
        raise LoadTestError(
            step,
            "mimetype differs: '%s' on the node, '%s' uploaded (%s)"
            % (mimetype, test_file.mimetype, test_file.basename),
            node_id=node_id,
        )


def wait_for_indexed(api, pending, timeout, interval):
    """
    Polls the search until all nodes show up in the index.

    pending: list of (node_id, keyword, expected properties).
    The tracker works asynchronously, so this is retried until the timeout is
    reached and only then gives up.

    The search only decides whether a node is indexed. The metadata is then
    checked through getMetadata of the node API, not taken from the search hit.
    """
    open_nodes = list(pending)
    deadline = time.time() + timeout
    started = time.time()
    attempts = 0
    last_hits = {}

    while open_nodes:
        attempts += 1
        still_open = []
        for node_id, keyword, expected in open_nodes:
            result = api.search_by_keyword(keyword)
            hits = {node["ref"]["id"]: node for node in result.get("nodes") or []}
            last_hits[node_id] = sorted(hits)
            if node_id in hits:
                # the search only tells us THAT the node is indexed; the metadata is
                # checked through the node API, not taken from the search hit
                fresh = api.get_metadata(node_id)
                verify(node_id, fresh["node"], expected, step="verify(nach-index)")
            else:
                still_open.append((node_id, keyword, expected))
        open_nodes = still_open

        if not open_nodes:
            break
        if time.time() >= deadline:
            node_id, keyword, _ = open_nodes[0]
            raise LoadTestError(
                "search",
                "node not in the search index after %.0fs (%d search runs): keyword '%s', "
                "%d of %d nodes still missing. Hits of the last search: %s"
                % (timeout, attempts, keyword, len(open_nodes), len(pending),
                   last_hits.get(node_id) or "none"),
                node_id=node_id,
            )
        time.sleep(min(interval, max(0.0, deadline - time.time())))

    return time.time() - started, attempts


# ------------------------------------------------------------------ main flow


def run_one(api, folder, files, iteration, run_id, options):
    """
    A single iteration.

    Phase 1: upload all files in parallel and write their metadata.
    Phase 2: wait until all nodes have arrived in the search index.
    """
    created = []
    pending_search = []
    warnings = []
    lock = threading.Lock()

    def process(index, test_file):
        if options.file_delay:
            # staggered start so the pipelines overlap differently
            time.sleep(options.file_delay * index)

        suffix = uuid.uuid4().hex[:8]
        name = "loadtest-%s-%03d-%s-%s%s" % (
            run_id, iteration, test_file.stem, suffix, test_file.extension)
        title = "Lasttest %s #%03d (%s)" % (run_id, iteration, test_file.basename)
        keyword = "loadtest-%s-%s" % (run_id, suffix)

        started = time.time()
        node_id = api.create_child(folder, name, title, options.obey_mds_create)
        with lock:
            created.append(node_id)

        api.upload_content(node_id, name, test_file.mimetype, test_file.data)

        steps = []
        for round_number in range(1, max(1, options.metadata_repeat) + 1):
            for step_name, properties in metadata_steps(run_id, iteration, keyword, test_file):
                label = step_name if options.metadata_repeat <= 1 else "%s#%d" % (step_name,
                                                                                 round_number)
                steps.append((label, properties))

        version_comment = "LOADTEST_UPDATE" if options.versioning else None

        def run_step(label, properties):
            updated = api.set_metadata(node_id, properties, options.obey_mds_update,
                                       step="changeMetadata(%s)" % label,
                                       version_comment=version_comment)

            # what counts is the state on the node, not the write call's response
            fresh = api.get_metadata(node_id)
            verify(node_id, fresh["node"], properties, step="verify(%s)" % label)

            # the response is only compared: if something is missing there that
            # getMetadata does return, that is an incomplete read-after-write in the
            # response - no data loss, so it is only a warning
            stale = missing_properties(updated["node"], properties)
            if stale:
                warnings.append("%s: response of %s incomplete - %s"
                                % (node_id, label, describe_missing(stale, properties)))

        if options.parallel_metadata:
            # all metadata calls at once on the SAME node -> writer against writer
            with ThreadPoolExecutor(max_workers=len(steps)) as meta_pool:
                meta_futures = [meta_pool.submit(run_step, label, properties)
                                for label, properties in steps]
                meta_errors = []
                for future in meta_futures:
                    try:
                        future.result()
                    except Exception as error:
                        meta_errors.append(error)
                if meta_errors:
                    raise meta_errors[0]
        else:
            for label, properties in steps:
                run_step(label, properties)

        expected = {}
        for _, properties in steps:
            expected.update(properties)

        # all steps together - this is where a lost update shows up
        fresh = api.get_metadata(node_id)
        verify(node_id, fresh["node"], expected, step="verify(re-read)")
        verify_content(node_id, fresh["node"], test_file)

        with lock:
            pending_search.append((node_id, keyword, expected))
        return test_file.extension.lstrip("."), time.time() - started

    workers = options.concurrency or len(files)
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [pool.submit(process, index, test_file)
                   for index, test_file in enumerate(files)]
        errors = []
        timings = []
        for future in futures:
            try:
                timings.append(future.result())
            except Exception as error:  # erste Exception gewinnt, Rest wird trotzdem abgewartet
                errors.append(error)
        if errors:
            raise errors[0]

    if options.verify_listing:
        # catches nodes that were created but are not (or no longer) listed in the folder
        present = api.list_children_ids(folder)
        missing = [node_id for node_id in created if node_id not in present]
        if missing:
            raise LoadTestError(
                "verify(listing)",
                "%d of %d nodes missing from the folder listing of %s: %s"
                % (len(missing), len(created), folder, ", ".join(missing)),
                node_id=missing[0],
            )

    index_seconds = None
    if options.search_timeout > 0:
        index_seconds, _ = wait_for_indexed(api, pending_search, options.search_timeout,
                                            options.search_interval)

    return created, timings, index_seconds, warnings


def dump_failure(error, iteration, created):
    print("\n" + "=" * 78, file=sys.stderr)
    print("ABORTED in iteration %d, step '%s'" % (iteration, error.step), file=sys.stderr)
    print("Time:   %s" % datetime.now().isoformat(timespec="seconds"), file=sys.stderr)
    print("Error:  %s" % error.message, file=sys.stderr)
    if error.node_id:
        print("Node:   %s" % error.node_id, file=sys.stderr)
    if created:
        print("Nodes of this iteration (not deleted):", file=sys.stderr)
        for node_id in created:
            print("  - %s" % node_id, file=sys.stderr)
    if error.response is not None:
        print("Request:  %s %s" % (error.response.request.method, error.response.url),
              file=sys.stderr)
        print("Status:   %d" % error.response.status_code, file=sys.stderr)
        body = error.response.text or ""
        try:
            body = json.dumps(error.response.json(), indent=2, ensure_ascii=False)
        except ValueError:
            pass
        print("Response:\n%s" % body[:4000], file=sys.stderr)
    print("=" * 78, file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("-u", "--user", default="admin")
    parser.add_argument("-p", "--password", default="admin")
    parser.add_argument("-n", "--iterations", type=int, default=100)
    parser.add_argument("--repository", default="-home-")
    parser.add_argument("--parent", default="-userhome-",
                        help="node below which the intermediate folder lives (default: -userhome-)")
    parser.add_argument("--folder", default="loadtest",
                        help="name of the intermediate folder (default: loadtest)")
    parser.add_argument("--no-folder", action="store_true",
                        help="do not use an intermediate folder, upload directly into --parent")
    parser.add_argument("--file", action="append", dest="files", metavar="PATH",
                        help="test file (repeatable); replaces the default files")
    parser.add_argument("--mini-txt", type=int, default=DEFAULT_MINI_TXT, metavar="N",
                        help="number of additional tiny text files (default: 2, 0 = none)")
    parser.add_argument("--txt-only", type=int, metavar="N", nargs="?", const=5,
                        help="upload only tiny text files, N of them (without a value: 5); "
                             "the large default files are dropped")
    parser.add_argument("--txt-size", type=int, default=DEFAULT_TXT_SIZE, metavar="BYTES",
                        help="size of the tiny text files in bytes (default: %d)" % DEFAULT_TXT_SIZE)
    parser.add_argument("--concurrency", type=int, default=0, metavar="N",
                        help="parallel pipelines per iteration (default: all files, 1 = sequential)")
    parser.add_argument("--timeout", type=float, default=120.0, help="HTTP timeout in seconds")
    parser.add_argument("--delay", type=float, default=0.0,
                        help="pause between iterations in seconds (default: 0)")
    parser.add_argument("--file-delay", type=float, default=0.0,
                        help="staggered start of the parallel pipelines in seconds (default: 0)")
    parser.add_argument("--search-timeout", type=float, default=120.0,
                        help="max. wait for the search index per iteration (default: 120s)")
    parser.add_argument("--search-interval", type=float, default=2.0,
                        help="poll interval of the search in seconds (default: 2)")
    parser.add_argument("--no-search", action="store_true",
                        help="skip the search step (upload + metadata only)")
    parser.add_argument("--versioning", action="store_true", default=True,
                        help="default: write metadata via POST /metadata?versionComment=... "
                             "(changePropertiesWithVersioning, the code path from DESP-851)")
    parser.add_argument("--no-versioning", dest="versioning", action="store_false",
                        help="use PUT /metadata instead (changeProperties, which retries "
                             "inside updateNodeNative)")
    parser.add_argument("--parallel-metadata", action="store_true",
                        help="send the metadata calls of one node at the same time instead of "
                             "one after another (provokes writer against writer on one node)")
    parser.add_argument("--metadata-repeat", type=int, default=1, metavar="N",
                        help="repeat the metadata step sequence N times per node (default: 1)")
    parser.add_argument("--verify-listing", action="store_true",
                        help="after each iteration, check that all nodes are listed in the folder")
    parser.add_argument("--keep", action="store_true",
                        help="do not delete nodes of successful iterations")
    parser.add_argument("--obey-mds-create", action="store_true",
                        help="use obeyMds=true when creating (default: false, so cm:name gets through)")
    parser.add_argument("--no-obey-mds-update", action="store_true",
                        help="use obeyMds=false on metadata updates (default: true, like the client)")
    args = parser.parse_args()

    args.search_timeout = 0.0 if args.no_search else args.search_timeout
    args.obey_mds_update = not args.no_obey_mds_update

    run_id = uuid.uuid4().hex[:6]
    api = NodeApi(args.base_url, args.user, args.password, args.repository, args.timeout)

    print("Load test %s against %s" % (run_id, args.base_url))
    print("Iterations: %d | user: %s" % (args.iterations, args.user))

    durations = []
    index_waits = []
    stale_responses = 0
    started_all = time.time()
    iteration = 0
    created = []

    try:
        if args.txt_only:
            # "small text files only" variant: as many tiny parallel uploads as possible
            files = [TestFile.mini_txt(number, args.txt_size)
                     for number in range(1, args.txt_only + 1)]
        else:
            files = [TestFile.from_path(path) for path in (args.files or DEFAULT_FILES)]
            files += [TestFile.mini_txt(number, args.txt_size)
                      for number in range(1, max(0, args.mini_txt) + 1)]
        if not files:
            raise LoadTestError("init", "no test files configured")

        total_bytes = sum(len(test_file.data) for test_file in files)
        print("Test files (%d in total, %s per iteration):" % (
            len(files),
            "%d B" % total_bytes if total_bytes < 1024 else "%.1f KB" % (total_bytes / 1024.0)))
        for test_file in files:
            print("  - %s" % test_file)

        if args.no_folder:
            folder = args.parent
            print("Target: %s (no intermediate folder)" % folder)
        else:
            folder, freshly_created = api.ensure_folder(args.parent, args.folder)
            print("Target: %s/%s -> %s%s" % (args.parent, args.folder, folder,
                                             " (newly created)" if freshly_created else ""))
        print("Parallel: %d pipeline(s) at a time%s" % (
            args.concurrency or len(files),
            "" if not args.file_delay else ", staggered by %.2fs each" % args.file_delay))
        print("Metadata: %s, %s%s" % (
            "POST with versionComment (changePropertiesWithVersioning)" if args.versioning
            else "PUT (changeProperties)",
            "in parallel per node" if args.parallel_metadata else "sequentially per node",
            "" if args.metadata_repeat <= 1 else ", repeated %dx" % args.metadata_repeat))
        print("Search: %s" % ("off" if args.search_timeout <= 0 else
                              "ngsearch on %s, timeout %.0fs, every %.1fs"
                              % (PROP_KEYWORD, args.search_timeout, args.search_interval)))
        print("-" * 78)

        for iteration in range(1, args.iterations + 1):
            started = time.time()
            created, timings, index_seconds, warnings = run_one(api, folder, files, iteration,
                                                                run_id, args)
            elapsed = time.time() - started
            for warning in warnings:
                print("  WARN: %s" % warning)
            stale_responses += len(warnings)
            durations.append(elapsed)
            if index_seconds is not None:
                index_waits.append(index_seconds)

            print("[%s] Iteration %3d/%d ok in %6.2fs  (%s%s)" % (
                datetime.now().strftime("%H:%M:%S"), iteration, args.iterations, elapsed,
                ", ".join("%s %.2fs" % (ext, seconds) for ext, seconds in timings),
                "" if index_seconds is None else ", index %.2fs" % index_seconds))

            if not args.keep:
                for node_id in created:
                    api.delete(node_id)
                created = []

            if args.delay:
                time.sleep(args.delay)

    except LoadTestError as error:
        dump_failure(error, iteration, created)
        return 1
    except requests.RequestException as error:
        dump_failure(LoadTestError("http", "%s: %s" % (type(error).__name__, error)),
                     iteration, created)
        return 1
    except KeyboardInterrupt:
        print("\nInterrupted by user in iteration %d" % iteration, file=sys.stderr)
        if created:
            print("Nodes of this iteration (not deleted): %s" % ", ".join(created),
                  file=sys.stderr)
        return 130

    total = time.time() - started_all
    print("-" * 78)
    print("All %d iterations ok in %.1fs (min %.2fs / avg %.2fs / max %.2fs per iteration)" % (
        args.iterations, total, min(durations), sum(durations) / len(durations), max(durations)))
    if index_waits:
        print("Wait for the search index: min %.2fs / avg %.2fs / max %.2fs" % (
            min(index_waits), sum(index_waits) / len(index_waits), max(index_waits)))
    if stale_responses:
        print("Incomplete responses to write calls: %d "
              "(data on the node was correct in each case)" % stale_responses)
    return 0


if __name__ == "__main__":
    sys.exit(main())
