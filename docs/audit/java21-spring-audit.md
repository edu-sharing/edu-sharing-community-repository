# Audit: Java 21 und Spring über alle Repos

Stand: Branch `maven/fixes/12.0`, nach der Umstellung auf `maven.compiler.release=21`.
Geprüft: `bom`, `deploy`, `repository`, `repository-antivirus`, `-cluster`, `-elastic`, `-kafka`, `-mongo`, `-remote`, `-repackaged`, `-transform`, `sdk`, `services-connector`, `services-rendering` (die beiden letzten sind PHP und nur im Maven-Packaging relevant).

Zahlen sind grep-Treffer (ohne `target/`, `node_modules/`, Frontend) und dienen der Größenordnung.

## 1. Leitplanke: Alfresco-Baseline

Core und alle Plugins laufen als AMP/Webapp im Image `edu_sharing-community-common-docker-alfresco-repository:26.1.0-1` (`deploy/docker/pom.xml`) auf Alfresco 26.1.0.61. Alfresco gibt die Bibliotheksversionen im Webapp-Kontext vor (Quelle: `alfresco-community-repo-26.1.0.61.pom`):

| Bibliothek | Alfresco 26.1 | `repository/pom.xml` | Bewertung |
|---|---|---|---|
| Spring Framework | 7.0.2 | 7.0.2 | passt, nicht eigenständig anheben |
| Spring Security | 7.0.0 | 7.0.0 | passt |
| Jackson | 2.17.2 | 2.17.2 (annotations 2.20 für Jackson 3) | passt |
| AspectJ | 1.9.22.1 | war 1.8.9 | **behoben** |
| jakarta.servlet-api | 6.1.0 | war 5.0.0 | **behoben** |
| Mockito | 5.18.0 | war 5.11.0 | **behoben** |
| HttpClient | 4.5.14 und 5.5 | HC4 in ca. 33 Dateien | Migration auf 5 möglich |

Konsequenz: Im Webapp-Kontext (`repository`, antivirus, cluster, elastic-Backend, kafka-Backend, mongo, remote, transform-Backend) sind Spring-Versionssprünge erst mit einem Alfresco-Upgrade sinnvoll. Modernisierung ist dort nur mit APIs möglich, die Spring 7.0.2 hat. Frei upgradebar sind nur die Standalone-Boot-Apps.

| Standalone-App | Boot | Anmerkung |
|---|---|---|
| `repository/Backend/transform` | 4.0.7 (Spring 7.0.8) | eigener Kontext, Abweichung von 7.0.2 ist unkritisch |
| `repository-transform/jodconverter/server` | 4.0.7 | |
| `repository-elastic/tracker` | 3.5.16 | |
| `repository-kafka/notification-service` | **3.0.2 (EOL)** | größtes Risiko |

## 2. Bereits behoben (Quick-Fixes)

| Repo | Problem | Fix |
|---|---|---|
| repository | `AbstractInterruptableJob`: `Thread.stop()` wirft seit JDK 20 immer `UnsupportedOperationException`, der Force-Stop-Pfad jeder unterbrechbaren Quartz-Jobs schlug fehl | nur noch `interrupt()` plus Warnlog; das Flag `forceStop` bleibt für die Veto-Logik im `JobHandler` |
| repository | `aspectjweaver` 1.8.9 überschrieb Alfrescos 1.9.22.1 (Java-21-Classfiles) | auf 1.9.22.1 |
| repository | `jakarta.servlet-api` 5.0.0 neben Spring 7 und Tomcat 11 | auf 6.1.0 |
| repository | Mockito-BOM 5.11.0, `mockito-inline` 5.2.0 (seit Mockito 5 leer), JUnit-Hardpins 5.9.3 | BOM 5.18.0, `mockito-inline` in den Modulen entfernt (Version kommt über die BOM), JUnit-Version aus dem Root |
| repository | `PersistentHandlerEdusharing`, `ImportCleanerIdentifiersList`: `HashMap` wird aus `parallelStream` beschrieben | `Collections.synchronizedMap` (Null-Semantik bleibt) |
| repository | geteilte statische `SimpleDateFormat` in `PersistentHandlerEdusharing`, `NodeContentGet`, `SitemapServlet` | pro Aufruf eigene Instanz |
| repository-elastic | `WorkspaceService` (Singleton) teilt einen `SimpleDateFormat` zwischen Tracker-Threads | pro Aufruf eigene Instanz |
| repository-elastic, -kafka | Compose: JVM-Flags (`-Xmx`, JMX) standen unter `command:` und landeten als Programmargumente, die JVM ignorierte sie | als `JAVA_OPTS` gesetzt; im Migrations-Container bleibt nur `--mode=migration-only` als Argument |
| repository-kafka | `--enable-preview` im Entrypoint ohne Bedarf; `openjdk:21-jdk` ist deprecated | Flag entfernt, Builder-Image `eclipse-temurin:21-jdk` (Property `docker.from.eclipse-temurin.21.jdk`) |
| repository-transform | springfox 3.0.0 in `jodconverter/server`, unmaintained, mit Boot 4 unvereinbar, im Code ungenutzt | Abhängigkeiten und Property entfernt |
| repository-transform | Heap 90 % neben drei LibreOffice-Prozessen im selben Container (2Gi) | Default `minPercentage` und `maxPercentage` 60 |
| sdk | Archetype-CI (`helm`, `docker`) mit `maven:3.8.4-jdk-8` | `maven:3.9.9-eclipse-temurin-21-noble` |

Nicht geändert, aber bewusst:
- `OAIConst.DATE_FORMAT` und `ServerConstants.DEFAULTDATEFORMAT` sind öffentliche, geteilte `DateFormat`s. Sie werden nur in Jobs und selten parallel genutzt, ein Umbau wäre eine API-Änderung.
- `SitemapServlet.DATE_FORMAT` ist jetzt `@Deprecated`, aber noch vorhanden.
- Im Debug-Overlay `2_plugin-elastic-debug.yml` ersetzt `JAVA_OPTS` (jdwp) das neue `JAVA_OPTS` aus der Common-Datei. Speicher- und JMX-Flags fehlen dort.
- Die Mirror-Verfügbarkeit von `eclipse-temurin:21-jdk` und `liberica-openjdk-debian:21.0.12` ist nicht geprüft.

## 3. Spring-Arbeitspakete (nicht umgesetzt)

Priorität von oben nach unten.

1. **notification-service Boot 3.0.2 → 3.5 / 4.x**
   - Dockerfile: `-Djarmode=layertools` wird zu `-Djarmode=tools extract --layers`, `org.springframework.boot.loader.JarLauncher` wird zu `...loader.launch.JarLauncher`.
   - Spring Kafka: `JsonSerializer/JsonDeserializer` sind ab Spring Kafka 4 deprecated (`JacksonJson*`). `trusted.packages=*` einschränken.
   - `spring-context-support` 6.0.5 und springdoc 2.1.0 mitziehen, `ApiExceptionHandler` auf `ProblemDetail` (RFC 9457).
   - Erst danach `spring.threads.virtual.enabled` (ab 3.2).
2. **Tracker Boot 3.5 → 4.x**
   - `jackson-jakarta-rs-json-provider` 2.18.3 an die Jackson-Version von Boot angleichen.
   - Cxf-BOM und Jersey-Versionen (3.1.2 vs 3.1.3) vereinheitlichen.
   - `starter-web` und `starter-webflux` gleichzeitig: prüfen, ob beides nötig ist.
   - `@ConfigurationProperties` als Records, `logging.structured.format.console`.
3. **Versionen im Webapp-Kontext** (innerhalb der Alfresco-Linie)
   - `spring-test` 3.2.17 (`alfresco/module/pom.xml`, mongo) auf 7.0.2.
   - `kafka-clients` 3.5.0 (`repository-kafka/Backend/pom.xml`) gegen `kafka.version` 3.7.0.
   - Micrometer 1.14.14 gegen das prüfen, was Alfresco und Tomcat mitliefern.
   - `jackson-module-kotlin` 2.12.7, Codehaus-Jackson 1.x (11 Dateien, z. B. `UserStatus`, `JobQueueScheduler`).
   - `axis` und `commons-lang` 2.6 auf Ablösung prüfen.
4. **API-Modernisierung mit Spring 7.0.2**
   - `RestTemplate` (2 Dateien, `TransformServiceStatic` erzeugt pro Aufruf eine neue Instanz) auf `RestClient` (in `IDMRestClient`, `RenderingTool` schon vorhanden), optional `@HttpExchange`.
   - Apache HttpClient 4 auf 5 (Alfresco liefert 5.5 mit).
   - Field-`@Autowired` (ca. 172) auf Konstruktor-Injection, `@RequiredArgsConstructor` wird bereits 197-mal genutzt.
   - `org.springframework.lang.Nullable` (3 Dateien) auf JSpecify, `PropertyPlaceholderConfigurer` in `custom-core-services-context.xml`.
   - `ThreadPoolTaskExecutor` in `SpringConfigRoot` (3 `@Async`-Methoden) als `SimpleAsyncTaskExecutor` mit `setVirtualThreads(true)`, sobald die Thread-Locals geklärt sind.
5. **Betrieb**: Observability (`ObservationRegistry`; `MeterRegistry` nur in 6 Dateien), CDS und Layered Jars für Tracker und Transform (`-Djarmode=tools`, `-XX:+AutoCreateSharedArchive`), AOT nur für die kleinen Boot-Apps realistisch.

Schon in Ordnung: Spring Security nutzt `SecurityFilterChain` und `requestMatchers` (kein `WebSecurityConfigurerAdapter`, `antMatchers`, `@EnableGlobalMethodSecurity`). Kein `javax.*` mehr für Jakarta-EE-Klassen, keine `ListenableFuture`, `AsyncRestTemplate`, `spring.factories`, Sleuth oder Spring Retry.

## 4. Java-21-Arbeitspakete (nicht umgesetzt)

**Virtual Threads.** Nirgends aktiv. Kandidaten (I/O-lastig): `NodeRunner` (OAI-Import), `UpdaterServiceImpl`, `Monitoring`, `RenderingTool`, `OAIPMHLOMImporter`, `ChunkedUploadServiceImpl`, `EduLocalTransformClient`, `NodeConvertExecutorProvider`, im Tracker `ThreadUtil` (ForkJoinPool mit 16 Threads).
Voraussetzungen im Alfresco-Kontext:
- `AuthenticationUtil`, Transaktion und `I18NUtil` sind Thread-Locals. Jeder Task muss `runAs` und `RetryingTransactionHelper` selbst aufbauen (`NodeRunner` tut das nur im `runAsSystem`-Zweig).
- 20 eigene `ThreadLocal`, darunter `Context`, `AuthenticationUtils`, `ApplicationInfoContextHolder`, `QueryUtils`. `DelegatingSecurityContextHolderStrategy` nutzt eine `InheritableThreadLocal`, deren Werte in Poolthreads veralten können.
- Pinning auf JDK 21: `synchronized` um I/O sollte zu `ReentrantLock` werden, u. a. `HandleServiceImpl:257`, `RemoteObjectService:164`, `AdminServiceImpl:480-533`, `CollectionDao:364`, `PersistentHandlerEdusharing:239/305/382`, `SearchServiceElastic:1614`, `ApplicationInfoList:133-170`, `JobHandler:440/545`, `AuthorityServiceImpl`, `KafkaAdmin:93`, `HazelcastLockManager`.
- Weitere Executor-Lecks: `NodeRunner` ruft `shutdown()` nur im Erfolgsfall auf (`ExecutorService` ist seit JDK 19 `AutoCloseable`), der `ForkJoinPool` in `UpdaterServiceImpl` wird nie beendet. `NodeRunner` führt im Default (`runAsSystem=false`) Tasks ohne Alfresco-Security-Kontext aus.
- Structured Concurrency und `ScopedValue` sind in 21 noch Preview.

**Sprache**
- Pattern Matching für `instanceof`: ca. 455 Stellen (`NodeServiceImpl` 23, `MCAlfrescoAPIClient` 20, `DAOException` 19, `ErrorResponse` 15); Pattern-`switch` für die Exception-Ladders in `DAOException` und `ErrorResponse`.
- Switch-Ausdrücke: 58 klassische `switch`, 184 `else if (...equals)`-Ketten (`MetadataReader`, `SearchServiceElastic`).
- Records: 37 Lombok-`@Value` (z. B. `ContributorData`, `NodeRelationData`), Kafka-Contract (`notification-data-contract`, 21 Dateien) über sealed Interfaces plus Records, ist aber eine Vertragsänderung. Mongo-Dokumente nur, wenn Codec und Treiber Records unterstützen.
- Text Blocks für SQL/XML: ca. 380 Zeilen mit `" +` (`ContributorMapper`, `ActivityStatisticService`, `EduFileFolderServiceImpl`).
- `Stream.toList()` statt `collect(Collectors.toList())` (254 in repository, je ca. 18 in elastic, kafka, mongo): Achtung, das Ergebnis ist unveränderlich.
- Sequenced Collections: `getFirst()` und `getLast()` an ca. 30 Stellen (`get(size()-1)`, `iterator().next()`).

**Veraltete APIs**

| API | Treffer |
|---|---|
| `new Integer/Boolean/Long(...)` (deprecated for removal) | 63 in repository, 7 in cluster (JLAN) |
| `new URL(...)` (deprecated seit 20) | 33 |
| `new Locale(...)` (deprecated seit 19) | 12 in repository, 5 in mongo |
| `Thread.getId()` (deprecated seit 19, `threadId()`) | 10 in repository, 3 in cluster |
| `Class.newInstance()` | 7 |
| `finalize()` | 2 (cluster) |
| `SimpleDateFormat` | 64 in 28 Dateien |
| `java.util.Date` | 257 in 151 Dateien |

`Thread.stop`, `SecurityManager`, `sun.*`-Imports: keine mehr.

## 5. Build und Runtime

- **Plugin-Versionen** kommen aus dem Super-POM `edu_sharing-super-pom` (liegt außerhalb dieser Repos; geprüft wurde die Kopie in `~/.m2`, Stand 2025-03-28): compiler 3.8.1, surefire 3.1.2, failsafe 3.0.0-M5, javadoc 3.3.1, shade 3.2.4 (ASM liest keine Java-21-Klassen), docker-maven-plugin 0.39.1, gmavenplus/groovy 3.0.9. Dort ist `maven.compiler.source/target` noch 1.8, jedes Repo überschreibt es jetzt mit `release=21`.
- **Compiler**: kein `-Xlint`. Vorschlag `-Xlint:deprecation,removal,unchecked`. Lombok in `annotationProcessorPaths` aufnehmen (JDK 23+ schaltet implizite Annotation Processors ab).
- **Tests**: Mockito 5 hängt sich unter JDK 21 selbst an und warnt; `-javaagent` in der surefire-`argLine` setzen (20 Testdateien nutzen `mockStatic`/`mockConstruction`). Test-Abhängigkeiten in `deploy` und `sdk` sind alt (JUnit 5.8.2, Testcontainers 1.16.3).
- **JVM-Flags** im Alfresco-Container (`entrypoint.sh`): `--add-modules java.se --add-exports java.base/jdk.internal.ref ...`; für Hazelcast fehlt `--add-opens java.base/java.nio`. Die JDK-internen xerces/xalan-Factories werden per System-Property erzwungen.
- **Container**:
  - Tracker und Transform laufen auf der vollen Liberica-JDK-Debian-Image, ein JRE- oder jlink-Image wäre kleiner.
  - Alle drei Images laden async-profiler 2.0 (2021); im Alpine-Image von notification-service ist die glibc-Variante nutzlos.
  - Es fehlen `-XX:+ExitOnOutOfMemoryError`, Heap-Dump und CDS.
  - Bei unter 2 CPUs wählt die JVM `SerialGC`, bei Bedarf `-XX:+UseG1GC` explizit setzen.
  - Tracker: 90 % Heap sind bei Netty-Direct-Memory knapp, 70-75 % sind sicherer.
  - Für notification-service gibt es keinen Helm-Chart, nur ein Pom.
- **CI**: alle Repos bauen mit `maven:3.9.9-eclipse-temurin-21`. Übrig sind `distroless/java:11` in `repository-elastic/.legacy` und `php:8.0` in `services-rendering/.legacy`.
