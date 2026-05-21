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

---

### Dynamic Node ID Mappings — `NodeDao.mapNodeConstants`
**File:** `Backend/services/core/src/main/java/org/edu_sharing/restservices/NodeDao.java`

Symbolic node names (e.g. `-userhome-`, `-inbox-`) that REST callers can use instead of real node IDs. Resolved in `mapNodeConstants(RepositoryDao, String, boolean)`.

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
| `ccm:page_variant_is_template`            | `"true"` / `"false"`                                  |
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
