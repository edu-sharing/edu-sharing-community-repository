---
name: java-modernization
description: Modernisiert und räumt Java-Klassen im edu-sharing-Backend nach den hauseigenen Konventionen auf — Lombok statt handgeschriebenem Boilerplate, parametrisiertes SLF4J-Logging, StringUtils statt manueller Null-Checks, Lambdas statt anonymer Klassen. Nutze diesen Skill immer, wenn der User eine bestehende Java-Klasse "aufräumen", "refactoren", "modernisieren", "cleanen", auf "lombok" umstellen oder das Logging vereinheitlichen will — auch wenn er nicht ausdrücklich "Skill" sagt.
---

# Java-Modernisierung (edu-sharing Backend)

Dieser Skill bündelt die wiederkehrenden Cleanup-Muster, mit denen Java-Klassen im
edu-sharing-Backend modernisiert werden. Ziel ist reine Lesbarkeit und Konsistenz —
**das Laufzeitverhalten darf sich nie ändern**.

## Grundregeln

- **Nur Cleanup, keine Logikänderung.** Wenn du gleichzeitig ein Feature einbaust,
  trenne das in einen eigenen Commit. Cleanup und Verhaltensänderung gehören nicht
  in denselben Diff.
- **Lombok ist bereits Projekt-Abhängigkeit** (`pom.xml` in `alfresco/*`, `services/*`),
  also gefahrlos einsetzbar.
- **Öffentliche Signaturen respektieren.** Lombok-`@Getter`/`@Setter` erzeugen exakt
  `getX()`/`setX()` (bzw. `isX()` für `boolean`) — die Namen bleiben kompatibel zu
  Spring-XML-Beans und externen Aufrufern. Nicht umbenennen.
- Wende ein Muster nur an, wo es die Lesbarkeit erhöht, nicht mechanisch um des
  Umstellens willen.

## Muster

### 1. Logging-Feld → `@Slf4j`

Handgehaltene Logger-Felder durch die Lombok-Klassenannotation ersetzen. Das von
`@Slf4j` erzeugte Feld heißt `log` (nicht `logger`).

**Vorher**
```java
import org.apache.log4j.Logger;

public class Foo {
    Logger logger = Logger.getLogger(Foo.class);
    ...
    logger.info("started");
}
```

**Nachher**
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Foo {
    ...
    log.info("started");
}
```

### 2. Parametrisiertes Logging

String-Konkatenation in Log-Aufrufen durch `{}`-Platzhalter ersetzen. So entsteht keine
String-Bildung, wenn das Log-Level deaktiviert ist, und die Aufrufe sind lesbarer.

**Vorher**
```java
logger.error(" username:" + userName + " applicationId:" + applicationId + " ( clientIp:" + clientIp + ")");
```

**Nachher**
```java
log.error(" username:{} applicationId:{} ( clientIp:{})", userName, applicationId, clientIp);
```

Ein konstanter Präfix-String darf konkateniert bleiben, der variable Teil wird zu `{}`:
`log.info(MSG + " {}", appInfo)`.

### 3. Null-/Leer-Prüfungen → `StringUtils.isBlank`

Manuelle Prüfungen auf leere Strings durch `StringUtils.isBlank(...)` ersetzen.

**Vorher**
```java
if (s == null || s.trim().length() == 0) { ... }
if (username == null || username.trim().equals("")) { ... }
```

**Nachher**
```java
if (StringUtils.isBlank(s)) { ... }
if (StringUtils.isBlank(username)) { ... }
```

> **Verbindlich `org.apache.commons.lang3.StringUtils` importieren.**
> Nicht `org.apache.tika.utils.StringUtils` verwenden — diese Variante hat sich
> versehentlich eingeschlichen und soll vereinheitlicht werden. Beim Aufräumen einer
> Klasse einen bestehenden Tika-Import auf commons-lang3 umstellen.

### 4. Handgeschriebene Getter/Setter → Lombok

Triviale Accessoren durch `@Getter`/`@Setter` ersetzen — am einzelnen Feld oder, wenn
fast alle Felder betroffen sind, an der Klasse. Felder dabei `private` machen und `final`,
wo sie nur einmal (im Konstruktor) zugewiesen werden.

**Vorher**
```java
List<AuthMethodInterface> ccAuthMethod;

/** IOC */
public void setCcAuthMethod(List ccAuthMethod) {
    this.ccAuthMethod = ccAuthMethod;
}
```

**Nachher**
```java
/**
 * -- SETTER --
 *  IOC
 */
@Setter
private List<AuthMethodInterface> ccAuthMethod;
```

Vorhandenes Javadoc des Setters wird mit der `-- SETTER --`-Konvention an die Annotation
übernommen, damit die Doku nicht verloren geht. Für `boolean`-Felder erzeugt `@Getter`
korrekt `isX()` — bestehende Aufrufer wie `appInfo.isAllowAdminLogin()` funktionieren
unverändert.

**`@Data` für vollständige POJOs.** Hat eine Klasse fast nur Felder mit trivialen Accessoren,
ersetzt `@Data` an der Klasse die einzelnen `@Getter`/`@Setter` (z. B. `ACE`-Objekt). `@Data`
bündelt `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor`.

> **Nur für reine DTO-/Wert-Klassen einsetzen.** `@Data` erzeugt zusätzlich
> `equals`/`hashCode`/`toString` — bei Entities mit Identitätssemantik oder Klassen, die in
> Sets/Maps als Schlüssel landen, kann das das Verhalten ändern. Im Zweifel bei
> `@Getter`/`@Setter` bleiben.

**Vererbung: `@EqualsAndHashCode(callSuper = true)` an der Subklasse.** `@Data` (bzw.
`@EqualsAndHashCode`) bezieht standardmäßig **nur die Felder der eigenen Klasse** in
`equals`/`hashCode` ein — die Felder der Oberklasse werden ignoriert. Trägt eine Subklasse
einer `@Data`-Klasse selbst `@Data` (oder `@EqualsAndHashCode`), muss sie zusätzlich
`@EqualsAndHashCode(callSuper = true)` erhalten, sonst gehen die Oberklassen-Felder im
Vergleich verloren (und Lombok warnt beim Kompilieren). Wird nur die Oberklasse auf `@Data`
umgestellt, prüfe daher **alle** Subklassen mit eigenem `@Data`/`@EqualsAndHashCode` und ziehe
`callSuper = true` mit.

```java
@Data
public class Usage { ... }

@Data
@EqualsAndHashCode(callSuper = true)   // sonst werden die Usage-Felder ignoriert
public static class NodeUsage extends Usage { ... }
```

**`boolean`-Feld-Naming `isX` → `x`.** Boolean-Felder ohne `is`-Präfix benennen. Lombok
`@Getter` erzeugt daraus weiterhin `isX()`, `@Setter` erzeugt `setX(boolean)` — Aufrufer
bleiben kompatibel.

**Vorher**
```java
private boolean isInherited;
public boolean isInherited() { return isInherited; }
public void setInherited(boolean isInherited) { this.isInherited = isInherited; }
```

**Nachher**
```java
@Getter @Setter
private boolean inherited;   // erzeugt isInherited() / setInherited(boolean)
```

### 5. Dekorative Block-Kommentare → Zeilenkommentare

`/** ... */`-Blöcke, die nur Codeabschnitte gliedern (kein echtes Javadoc an einem
Member), zu schlichten `//`-Kommentaren machen.

**Vorher**
```java
/**
 * check params
 */
if (...) { ... }
```

**Nachher**
```java
// check params
if (...) { ... }
```

### 6. Anonyme Klassen → Lambdas

Funktionale Einzelmethoden-Interfaces als Lambda schreiben; überflüssige lokale
Variablen und auto-generierte TODO-Kommentare entfernen.

**Vorher**
```java
AuthenticationUtil.RunAsWork<String> runAs = new AuthenticationUtil.RunAsWork<String>() {
    @Override
    public String doWork() throws Exception {
        // TODO Auto-generated method stub
        NodeRef ref = personService.getPersonOrNull(fUserName);
        String repoUsername = (String) nodeService.getProperty(ref, ContentModel.PROP_USERNAME);
        return repoUsername;
    }
};
String repoUsername = AuthenticationUtil.runAsSystem(runAs);
```

**Nachher**
```java
String repoUsername = AuthenticationUtil.runAsSystem(() -> {
    NodeRef ref = personService.getPersonOrNull(fUserName);
    return (String) nodeService.getProperty(ref, ContentModel.PROP_USERNAME);
});
```

### 7. Konkrete Map-Implementierung → `Map`-Interface

Felder, lokale Variablen, Methoden-Signaturen und Casts auf das Interface (`Map` /
`ConcurrentMap`) abstützen statt auf die Implementierung (`HashMap` / `ConcurrentHashMap`).
Die Konstruktion bleibt `new HashMap<>()` — nur der deklarierte bzw. referenzierte Typ wird
zum Interface. Dabei den Diamond-Operator nutzen.

**Vorher**
```java
HashMap<Long, CacheEntry> m = new HashMap<Long, CacheEntry>();
ConcurrentHashMap<Long, Long> added = (ConcurrentHashMap<Long, Long>) cache.lookup(KEY);
```

**Nachher**
```java
Map<Long, CacheEntry> m = new HashMap<>();
ConcurrentMap<Long, Long> added = (ConcurrentMap<Long, Long>) cache.lookup(KEY);
```

Wird nach der Umstellung nur noch der Interface-Typ referenziert, den Import
`java.util.concurrent.ConcurrentHashMap` auf `java.util.concurrent.ConcurrentMap` umstellen
(bzw. `java.util.Map` ergänzen). Gilt analog für Rückgabe- und Parametertypen öffentlicher
Methoden — diese bleiben kompatibel, weil `HashMap` das `Map`-Interface implementiert.

### 8. Nullability-Annotationen → `org.jetbrains.annotations`

`@Nullable`/`@NotNull` auf Return-Typen und Parameter setzen, wo Null-(Un)zulässigkeit
dokumentiert werden soll. Verbindlich die JetBrains-Variante verwenden.

**Vorher**
```java
import com.drew.lang.annotations.Nullable;
```

**Nachher**
```java
import org.jetbrains.annotations.Nullable;
```

> **Verbindlich `org.jetbrains.annotations.{Nullable,NotNull}` importieren.**
> Fehlerhaft eingeschlichene Varianten wie `com.drew.lang.annotations.*` beim Aufräumen einer
> Klasse umstellen — dieselbe Vereinheitlichungsregel wie bei `StringUtils` (Muster 3). Die
> `org.jetbrains:annotations`-Dependency muss im jeweiligen Modul-`pom.xml` vorhanden sein
> (in `alfresco/module` bereits ergänzt); bei Bedarf dort eintragen.

### 9. Import- & Dead-Code-Hygiene

- Ungenutzte Imports entfernen (z. B. den alten `org.apache.log4j.Logger` nach Muster 1).
- Gruppierung: Lombok- und Drittanbieter-Imports zuerst, `java.*`/`javax.*` zuletzt.
- Keine Wildcard-Imports neu einführen.
- Ungenutzte lokale Variablen entfernen.
- Redundante Methodenaufrufe ohne Wirkung entfernen (z. B. ein `getX()`, dessen Rückgabe
  verworfen wird und der keinen Seiteneffekt hat).
- Überflüssige `throws Exception`/`throws Throwable` aus Signaturen entfernen, wenn keine
  geprüfte Exception (mehr) geworfen wird — dabei Interface-Deklarationen und alle Aufrufer
  mitziehen.

> **Vor dem Entfernen prüfen:** Ein Aufruf darf nur weg, wenn er nachweislich seiteneffektfrei
> ist; ein `throws` nur, wenn kein Aufrufer die geprüfte Exception erwartet. Im Zweifel den
> Diff gegen das Original prüfen.

### 10. Felder finalisieren & ordnen

**`final`, wo möglich.** Felder, die nach der Konstruktion nie neu zugewiesen werden, `final`
machen — insbesondere injizierte Service-Felder (`@RequiredArgsConstructor`-Injection) und
Konstanten. Das dokumentiert Unveränderlichkeit und lässt den Compiler versehentliche
Neuzuweisungen verhindern. Nur Felder, die tatsächlich nach der Initialisierung neu gesetzt
werden, bleiben veränderlich.

**Felder gruppieren — gleiche Art ohne Leerzeilen zusammen, Gruppen durch eine Leerzeile
getrennt.** Reihenfolge der Gruppen:

1. `static`-Felder (Konstanten, statische Logger usw.)
2. Injizierte Service-/Abhängigkeits-Felder
3. Übrige Instanzfelder (Zustand)

Innerhalb jeder Gruppe `public` vor `private` (und `protected` dazwischen).

**Vorher**
```java
private NodeService nodeService;

public static final String PREFIX = "ccm";

private PermissionService permissionService;

private static Logger logger = Logger.getLogger(Foo.class);

private String state;
```

**Nachher**
```java
public static final String PREFIX = "ccm";
private static final Logger logger = LoggerFactory.getLogger(Foo.class);

private final NodeService nodeService;
private final PermissionService permissionService;

private String state;
```

Wird der Logger ohnehin auf `@Slf4j` umgestellt (Muster 1), entfällt das statische
Logger-Feld ganz. Bei der Umsortierung keine Initialisierer oder Annotationen an Feldern
verlieren — nur Reihenfolge, Leerzeilen und `final` ändern, sonst nichts.

## Vorgehen bei einer Klasse

1. Datei lesen, Muster 1–6 sowie die mechanischen Cleanups (Muster 7 Map-Interface,
   Muster 8 Nullability-Imports) identifizieren.
2. Änderungen anwenden, dabei jede entfernte Methode/jedes Feld/jeden `throws` auf
   verbleibende Verwendungen und Aufrufer prüfen.
3. Imports und toten Code bereinigen (Muster 9).
4. Felder finalisieren und ordnen (Muster 10).
5. Sicherstellen, dass keine Verhaltensänderung entstanden ist — bei Unsicherheit den
   Diff gegen das Original prüfen.
