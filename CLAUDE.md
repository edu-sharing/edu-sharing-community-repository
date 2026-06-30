# edu-sharing Repository — Backend Developer Guide

## Module Structure

| Module               | Path                         | Purpose                                                                      |
|----------------------|------------------------------|------------------------------------------------------------------------------|
| `alfresco/common`    | `Backend/alfresco/common`    | Shared constants, DTOs, utilities used by all modules                        |
| `alfresco/module`    | `Backend/alfresco/module`    | Alfresco AMP — policies, interceptors, metadata engine, MDS query processing |
| `services/core`      | `Backend/services/core`      | REST layer, DAOs, search services, node services, admin tools                |
| `services/rendering` | `Backend/services/rendering` | Rendering service                                                            |
| `config/defaults`    | `config/defaults`            | Default MDS XML definitions, i18n properties, Lightbend config               |

**Dependency direction:** `services/core` → `alfresco/module` → `alfresco/common`. Never import `services/core` classes from `alfresco/module`.

---

## Key Patterns

### Constants — `CCConstants`
**File:** `Backend/alfresco/common/src/main/java/org/edu_sharing/repository/client/tools/CCConstants.java`

Single source of truth for all Alfresco property names, aspect names, type names, permission strings, and i18n keys (~2 400 lines).

#### Two naming forms — both stored as separate constants

Every property exists in a *global* (long) form used by the Alfresco NodeService and a *local* (short) form used in the REST API, MDS XML, and the Elasticsearch index:

| Form          | Pattern                    | Example                                        |
|---------------|----------------------------|------------------------------------------------|
| Global (long) | `{namespace-uri}localname` | `{http://www.campuscontent.de/model/1.0}title` |
| Local (short) | `prefix:localname`         | `ccm:title`                                    |

Conversion helpers:
```java
CCConstants.getValidLocalName(globalName)   // {ns}prop  →  prefix:prop
CCConstants.getValidGlobalName(localName)   // prefix:prop  →  {ns}prop
```

#### Namespace prefixes

| Prefix    | Full URI                                    | Domain                                   |
|-----------|---------------------------------------------|------------------------------------------|
| `ccm`     | `http://www.campuscontent.de/model/1.0`     | edu-sharing custom model                 |
| `cm`      | `http://www.alfresco.org/model/content/1.0` | standard Alfresco content model          |
| `cclom`   | `http://www.campuscontent.de/model/lom/1.0` | LOM educational metadata                 |
| `sys`     | `http://www.alfresco.org/model/system/1.0`  | Alfresco system properties               |
| `virtual` | `virtualproperty` *(literal, not a URI)*    | computed/virtual properties — not stored |

#### Constant families

| Prefix                              | What it covers                                                                                                |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `CCM_TYPE_*`                        | Node type QNames — `CCM_TYPE_IO` (learning object), `CCM_TYPE_MAP` (folder/collection), `CCM_TYPE_COMMENT`, … |
| `CCM_ASPECT_*`                      | Aspect QNames — collection, published, tracking, gdpr, page_variant, …                                        |
| `CCM_PROP_*`                        | edu-sharing property QNames — largest group                                                                   |
| `CCM_PROP_IO_REPL_*`                | Flattened LOM educational metadata replicated onto IO nodes (contributor roles, taxon paths, …)               |
| `CCM_VALUE_*`                       | Allowed values / enum strings for CCM properties                                                              |
| `CCM_VALUE_MAP_TYPE_*`              | Discriminator values stored in `CCM_PROP_MAP_TYPE` to classify map/folder nodes                               |
| `CCM_VALUE_TOOLPERMISSION_*`        | Feature-gate strings for the tool-permission system                                                           |
| `CM_*`                              | Standard Alfresco Content Model properties (`CM_NAME`, `CM_PROP_C_TITLE`, …)                                  |
| `LOM_PROP_*`                        | LOM property QNames (title, version, educational context, lifecycle, …)                                       |
| `VIRT_PROP_*`                       | Virtual (computed) property keys — namespace is the literal string `virtualproperty`                          |
| `PERMISSION_*`                      | Node-level ACL permission strings (`Read`, `Write`, `Delete`, `CCPublish`, …)                                 |
| `AUTHORITY_*`                       | Role and authority name strings                                                                               |
| `I18n_SYSTEMFOLDER_*`               | i18n lookup keys for system folder display names                                                              |
| `NAMESPACE_*` / `NAMESPACE_SHORT_*` | Full namespace URIs and their short prefixes                                                                  |
| `AUTH_*`                            | Authentication session / token keys                                                                           |
| `SESSION_*`                         | HTTP session attribute keys — constant name equals value (e.g. `SESSION_RENDERING_DETAILS = "SESSION_RENDERING_DETAILS"`) |
| `COMMON_LICENSE_*`                  | License type keys and Creative Commons URL templates                                                          |

#### Virtual properties (`VIRT_PROP_*`)

Not persisted in Alfresco. Added to the properties map at read time by `PropertiesGetInterceptor` implementations. Examples: `{virtualproperty}mediatype`, `{virtualproperty}permalink`, `{virtualproperty}usagecount`. In MDS XML they are referenced with the `virtual:` prefix (e.g. `virtual:mediatype`).

#### System folder types (`CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_*`)

Values stored in `CCM_PROP_MAP_TYPE` on folder nodes inside `Company Home / Edu_Sharing_System /`. All carry the prefix `EDUSYSTEM_`. When adding a new system folder, add **both** a `CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_<NAME>` constant **and** a matching `I18n_SYSTEMFOLDER_<NAME>` constant.

#### Tool permissions vs. node permissions

Two independent systems — do not conflate:
- **`PERMISSION_*`** — control access to a **node** (Alfresco ACL: `Read`, `Write`, `Delete`, `CCPublish`, …). Checked via `PermissionService`.
- **`CCM_VALUE_TOOLPERMISSION_*`** — control access to a **feature** regardless of node (e.g. `TOOLPERMISSION_INVITE`, `TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH`). Checked via `ToolPermissionService`.

---

### System Folders — `UserEnvironmentTool`
**File:** `Backend/services/core/src/main/java/org/edu_sharing/repository/server/tools/UserEnvironmentTool.java`

Admin-managed folders that live under `Company Home / Edu_Sharing_System /`.

**Adding a new system folder** (follow the `reports` pattern):
1. Add two constants to `CCConstants` (see above).
2. Add an i18n entry to both property files:
   - `config/defaults/src/main/resources/metadatasets/i18n/mds.properties`
   - `config/defaults/src/main/resources/metadatasets/i18n/mds_de_DE.properties`
3. Add a public getter to `UserEnvironmentTool`:
   ```java
   public String getEdu_Sharing<Name>Folder() {
       return getOrCreateSystemFolderByName(
           CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_<NAME>,
           CCConstants.I18n_SYSTEMFOLDER_<NAME>);
   }
   ```
4. Register it in `createAllSystemFolders()`.

**Seeding default children inside a system folder** — if a folder needs a default child node at creation time, override the getter and call `getOrCreateChildMap` after `getOrCreateSystemFolderByName`. Add a `CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_<NAME>_DEFAULT` constant (value `EDUSYSTEM_<NAME>_DEFAULT`) and a matching `I18n_SYSTEMFOLDER_<NAME>_DEFAULT` key. Pass extra properties (e.g. aspect flags) via the `additionalProps` overload:
```java
public String getEdu_Sharing<Name>Folder() {
    String folderId = getOrCreateSystemFolderByName(...);
    getOrCreateChildMap(folderId, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_<NAME>_DEFAULT,
        CCConstants.I18n_SYSTEMFOLDER_<NAME>_DEFAULT,
        Map.of(CCConstants.CCM_PROP_PAGE_VARIANT_IS_TEMPLATE, true));
    return folderId;
}
```

**Non-mandatory aspects when creating nodes** — Alfresco automatically applies an aspect when any of its properties is written, so simply including a property from a non-mandatory aspect (e.g. `ccm:page_variant_is_template` from `ccm:page_variant`) in the props map is sufficient. If you need to apply an aspect without setting a property, call `nodeService.setAspects(...)` explicitly.

---

### Dynamic Node ID Mappings — `NodeDao.mapNodeConstants`
**File:** `Backend/services/core/src/main/java/org/edu_sharing/restservices/NodeDao.java`

Symbolic node names (e.g. `-userhome-`, `-inbox-`, `-collectionhome-`) that REST callers can use instead of real node IDs. Resolved in `mapNodeConstants(RepositoryDao, String, boolean)`. Every new symbolic constant must be declared as a `public static final String NODE_CONSTANT_*` field on `NodeDao` and resolved inside `mapNodeConstants` — **never** inline at the call site. When a method needs to know whether a caller originally passed a symbolic constant (e.g. to branch on it after resolution), capture the check **before** calling `mapNodeConstants`.

---

### Spring Application Contexts — Two Separate Roots

There are two independent Spring application contexts; using the wrong one will fail to find the bean:

| Context | Accessor | Contains |
|---------|----------|---------|
| Alfresco (AMP) | `AlfAppContextGate.getApplicationContext()` | Alfresco core beans — `ServiceRegistry`, caches, policies |
| edu-sharing webapp | `ApplicationContextFactory.getApplicationContext()` | `services/core` Spring beans — `PermissionChecking`, `ActivityEventService`, `GlobalShareService`, … |

---

### Context-Aware Services — `AppContextServiceFactory`

Services that must resolve a different implementation per repository (local vs. remote providers like DDB, Brockhaus, …) are obtained through a typed factory instead of `getBean(...)`:

```java
SearchService s = SearchServiceFactory.getInstance().getService(repoDao.getId()); // by repo/app id
SearchService s = SearchServiceFactory.getInstance().getLocalService();           // home repo
```

**Adding a new context-aware service** (pattern mirrored by `SearchService`/`ContributorService`):
1. Declare an interface `XServiceFactory extends AppContextServiceFactory<XService>` with a static `getInstance()` (see `SearchServiceFactory`). `AppContextLocatorAutoRegistrar` auto-registers a proxy bean for any such interface under `org.edu_sharing` — **no XML/bean config needed**.
2. Provide a fallback no-op `XServiceAdapter` (`@Lazy @Service implements XService`) for contexts without a real impl (see `SearchServiceAdapter`).
3. Register the impl(s) in **`AppContextConfig.java`** (the `AppContextRegistry` builder): `fallbackAppContext()` → adapter, `localAppContext()` + `ElasticSearchProvider` → real impl, other providers fall through to the adapter.

**Gotcha — register AOP-proxied beans by name, not by class.** `defineBean(XService.class, XServiceImpl.class)` resolves via `getBean(XServiceImpl.class)`. If the impl carries `@Permission`/`@HasRole` it is wrapped in a **JDK dynamic proxy** on the interface, which is *not* assignable to `XServiceImpl` → `NoSuchBeanDefinitionException` at the first `getService(...)` call (startup validation may not catch it). Register such beans by their bean name instead: `defineBean(XService.class, "xServiceImpl")` (default name = decapitalized class name). This is why `nodeService`, `permissionService`, `contributorServiceImpl`, … are referenced as strings while non-proxied impls/adapters use `.class`.

---

### `@NodePermission` on Static Methods

The `@NodePermission` annotation is processed by `PermissionChecking` via AOP, which **does not intercept static methods**. On a static method, add the annotation to the parameter for documentation, then enforce it manually:

```java
ApplicationContextFactory.getApplicationContext().getBean(PermissionChecking.class)
    .checkNodePermissions(nodeId, new String[]{CCConstants.PERMISSION_WRITE});
```

`PermissionChecking` is a Spring `@Aspect @Component` (proxy-based AOP), so the same annotations (`@Permission`, `@HasRole`, `@NodePermission`) are **only enforced on calls that go through the bean proxy** — i.e. external callers. A **self-invocation** (one method in a bean calling another annotated method on `this`) bypasses the check entirely. Gotcha: a `@Queued`/async method that internally calls an annotated getter does **not** trigger that getter's `@Permission`; enforce the toolpermission explicitly at the entry point (e.g. `ToolPermissionHelper.throwIfToolpermissionMissing(...)`) instead of relying on the annotation.

---

### MDS (Metadata Sets) — Search Queries
**File:** `config/defaults/src/main/resources/metadatasets/xml/mds.xml`

Elasticsearch queries are declared as `<query>` elements. Each `<property>` within a query maps to a search criterion the caller can pass.

**Value-specific statement** (only activates for exact value `"true"`):
```xml
<property name="virtual:page_variant_global">
  <statement value="true">{"match":{"path":"${systemfolder}"}}</statement>
</property>
```

**Useful ES fields in the index:**
| Field | Type | Usage |
|-------|------|-------|
| `path` | multi-value keyword | ancestor node IDs — `match` on this finds all descendants |
| `fullpath` | string | slash-separated ID path — use with `wildcard` |
| `properties.<qname>.keyword` | keyword | default property field |

**`path` ancestor match pattern** (used to scope a query to a subtree):
```json
{"match":{"path":"<nodeId>"}}
```
This is the same mechanism used by `exclude_system_folder` in `SearchServiceElastic`.

---

### Query Template Variables — `QueryUtils`
**File:** `Backend/alfresco/module/src/main/java/org/edu_sharing/metadataset/v2/QueryUtils.java`

`replaceCommonQueryParams` replaces well-known placeholders in every MDS statement string before the statement is used to build an ES query.

| Placeholder       | Resolves to                                     |
|-------------------|-------------------------------------------------|
| `${user.<prop>}`  | property from the authenticated user's info map |
| `${educontext}`   | current edu-sharing scope                       |
| `${authority}`    | fully-authenticated username                    |
| `${authorities}`  | pipe-separated list of all user authorities     |
| `${systemfolder}` | node ID of the Edu_Sharing_System base folder   |

`${systemfolder}` is resolved via `getChildByName` on company home and cached in a static volatile field. It uses only `alfresco/module`-level APIs (`Repository`, `ServiceRegistry`, `I18nServer`) to avoid a circular dependency on `services/core`.

---

### `page_variant` Query
Searches for nodes with the `ccm:page_variant` aspect. Key properties:

| Property                                  | Notes                                                 |
|-------------------------------------------|-------------------------------------------------------|
| `ccm:page_variant_is_template`            | `d:boolean` — pass `Boolean.TRUE`, **not** `"true"`   |
| `ccm:page_variant_profiling_target_group` | multi-value OR: `learner`, `teacher`, `general`       |
| `ccm:educationalcontext`                  | multi-value OR                                        |
| `virtual:page_variant_global`             | `"true"` → restricts to nodes under the system folder |

---

## Alfresco Caches

Caches are declared in two places; both must stay in sync.

**Bean declaration** — `Backend/alfresco/module/src/main/amp/config/alfresco/extension/custom-cache-context.xml`

```xml
<bean name="eduSharingTransformerCache" factory-bean="cacheFactory" factory-method="createCache">
    <constructor-arg value="cache.eduSharingTransformerCache"/>
</bean>
```

**Properties** — `config/defaults/src/main/resources/caches.properties`

```properties
cache.eduSharingTransformerCache.eviction-policy=LRU
cache.eduSharingTransformerCache.maxIdleSeconds=0
cache.eduSharingTransformerCache.maxItems=10000
cache.eduSharingTransformerCache.timeToLiveSeconds=60
```

**Java lookup** — use `AlfAppContextGate` (same pattern as `ConfigServiceImpl`):

```java
private static final SimpleCache<String, String> transformerCache =
    AlfAppContextGate.getApplicationContext().getBean("eduSharingTransformerCache", SimpleCache.class);
```

Naming convention: all cache beans use the `eduSharing*` prefix.

---

## BAPI Proxy Integration

**Service**: `BApiProxyService.forwardRequest(path, body, headers, method)` — `headers` may be `null` (the service already guards against it).

---

## Node Annotation AOP System

`@NodeManipulation` triggers `NodeOriginalAspect` which rewrites annotated parameters before the method body runs.

| Annotation               | Dispatch                                   | Resolves                                                                     |
|--------------------------|--------------------------------------------|------------------------------------------------------------------------------|
| `@NodeOriginal`          | `nodeService.getOriginalNode(id)`          | Collection references **and** published copies (`ccm:io_published_original`) |
| `@NodeReferenceOriginal` | `nodeService.getReferenceOriginalNode(id)` | Collection references only (`ccm:collection_io_reference → ccm:original`)    |

Use `@NodeReferenceOriginal` when a published copy must retain its own identity (e.g. fulltext extraction — the copy may have different binary content than the original).

`getOriginalNode` is implemented as: `getReferenceOriginalNode` first, then published-copy resolution on top.

---

## Alfresco Policy Patterns

### OnContentPropertyUpdatePolicy vs OnContentUpdatePolicy

Prefer `OnContentPropertyUpdatePolicy` when you need to react only to a specific content property (e.g. `cm:content`) and must **not** re-fire when other content properties are written (e.g. `ccm:fulltext_content`).

`OnContentUpdatePolicy` fires for any content property on the type, which causes a self-triggering loop if the policy also writes a content property.

---

## Privileged Writes Inside Policies / Services

Pattern for writes that need to bypass permission checks and run in their own transaction:

```java
AuthenticationUtil.runAsSystem(() ->
    serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
        // write operations
        return null;
    })
);
```

`serviceRegistry` is obtained via:

```java
static ServiceRegistry serviceRegistry =
    (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
```

---

## REST Authentication Filter — `ApiAuthenticationFilter`

**File:** `Backend/services/core/src/main/java/org/edu_sharing/restservices/ApiAuthenticationFilter.java`

Processes every `/rest/*` request. Supports Basic auth, Bearer (OAuth2), and EDU-Ticket auth. Key gotcha: `authTool.createNewSession(username, password)` sets the **Alfresco security-context thread-local** immediately — even if you do not subsequently call `storeAuthInfoInSession`. This means the backend endpoint may see the authenticated user's `authorityName` even if the HTTP session was never fully established (e.g. when 2FA is pending). Do not rely on `authorityName` alone to determine whether a session is fully valid; check `isValidLogin` in the returned `LoginInfo`.

Internal Tomcat port detection uses `httpReq.getLocalPort()` compared to `ApplicationInfoList.getHomeRepository().getPort()` (returns `String`, not `int`). Requests arriving on the internal port bypass 2FA checks.

---

## i18n

Backend i18n uses plain `.properties` files loaded by `I18nServer`.
**Files:**
- `config/defaults/src/main/resources/metadatasets/i18n/mds.properties` (default / EN)
- `config/defaults/src/main/resources/metadatasets/i18n/mds_de_DE.properties` (DE)

Keys are referenced via `CCConstants.I18n_*` constants and resolved with:
```java
I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE)
I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE, "de_DE")
```
