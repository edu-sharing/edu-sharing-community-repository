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

### 7. Import-Hygiene

- Ungenutzte Imports entfernen (z. B. den alten `org.apache.log4j.Logger` nach Muster 1).
- Gruppierung: Lombok- und Drittanbieter-Imports zuerst, `java.*`/`javax.*` zuletzt.
- Keine Wildcard-Imports neu einführen.

## Vorgehen bei einer Klasse

1. Datei lesen, Muster 1–6 identifizieren.
2. Änderungen anwenden, dabei jede entfernte Methode/jedes Feld auf verbleibende
   Verwendungen prüfen.
3. Imports bereinigen (Muster 7).
4. Sicherstellen, dass keine Verhaltensänderung entstanden ist — bei Unsicherheit den
   Diff gegen das Original prüfen.
