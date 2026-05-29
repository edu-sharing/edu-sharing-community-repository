# AI Agent

## Overview

This agent assists with development tasks in this Angular frontend project.

## Capabilities

-   Answer questions about the codebase
-   Help write and refactor TypeScript/Angular code
-   Debug errors and suggest fixes
-   Review components, services, and modules
-   Generate tests

## Usage

## Project Context

-   **Framework**: Angular
-   **Language**: TypeScript
-   **Test runner**: Jest / Playwright
-   **Styling**: SCSS

## Linting & Styles

-   Obey the tslint rules
-   if, loops and other containers should always have braces

## Key Areas

### Mds (Metadatasets)

-   Location: `src/app/features/mds` and `src/app/features/mds/mds-editor/widgets`
-   Handles widget definitions and dynamic frontend templating driven by metadata configurations

### General notes

-   use custom `esIcon` directive instead of `mat-icon`
-   Connector display names are translated via the `CONNECTOR.<id>.NAME` i18n key, where `<id>` is the connector id (e.g. `ONLYOFFICE`) from `RestConnectorsService.connectorSupportsEdit(node)?.id`. Translate it in the template, not in TS.
-   To override `mat-button` label color in a dark/overlay context, use the MDC token `--mdc-text-button-label-text-color` (e.g. `button.mat-mdc-button { --mdc-text-button-label-text-color: #fff; }`). The project uses `angular-material-css-vars.
-   `NodeHelperService.getOriginalId(node)` (in `src/app/services/node-helper.service.ts`) returns the `ccm:original` property value when present, falling back to `node.ref.id`. Use it to detect collection references and submission file variant content nodes.
-   Do NOT use spread syntax (`[...x]`, `{...x}`) in templates — it is only supported in Angular 21.1+ and this project is on an older version. A `Set`/`Map` won't serialize with the `json` pipe either (renders `{}`); convert to an array in the component if you need to inspect it.
-   `UIService.editConnector(node, type?, win?, connectorType?)` and `openConnector(...)` return the opened `Window`. Pre-open the window in the user-gesture (synchronously) and pass it in if the connector call follows an `await`, to avoid popup blockers.
-   Node list overlays: `es-node-entries-wrapper` projects an `<ng-template #overlay let-element="element">` (via `@ContentChild('overlay')`) rendered per card for `Grid`/`SmallGrid` display types. The card's `.card-overlay` provides the positioning context.
-   `es-dropdown` (in `projects/edu-sharing-ui/.../dropdown`) exposes its `MatMenu` as a `@ViewChild('dropdown') menu` and renders only a hidden `matMenuTrigger`. To trigger it from an external button: `<button [matMenuTriggerFor]="ref.menu">…</button>` where `ref` is the `#templateRef` on `<es-dropdown>`. Mobile fallback: call `ref.triggerBottomSheet()`.
-   `AddWithConnectorDialog` opens its popup `Window` synchronously inside the dialog's "Create" handler (user gesture) and returns it on `AddWithConnectorDialogResult.window`. Always forward that window to `UIService.editConnector(node, {win, connectorType})` — do not re-open one yourself, or the popup blocker will kill it.

### Layout — Editorial Page

-   `.main-content` uses `min-height` (body scrolls) by default. When a full-screen sub-component is active, toggle `[class.has-main-component]="mainComponent$ | async"` on `.main-content`; the `.has-main-component` rule switches to `display: flex; flex-direction: column; overflow: hidden; height: calc(100vh - margins)` so inner `flex: 1` children can fill and scroll internally.
-   For a component hosted inside `.main-content.has-main-component` to fill the available height and scroll internally: set `:host { display: flex; flex-direction: column; flex: 1; min-height: 0; overflow: hidden }`, and give every flex child in the scroll chain `flex: 1; min-height: 0` (the `min-height: 0` override is required — flex items default to `min-height: auto` which prevents `overflow-y: auto` from activating).
-   `mat-tab-group` inside such a component needs `flex: 1; min-height: 0` as well; Angular Material's `.mat-mdc-tab-body-content` already has `overflow-y: auto` and will scroll once it has a bounded height.

### Layout — Mobile bottom bar

-   `es-main-menu-bottom` becomes `position: fixed; bottom: 0; width: 100%` at `max-width: $mobileTabSwitchWidth` (= `$mobileWidth + $mobileStage * 2` = 900 px by default; see `projects/edu-sharing-ui/assets/scss/variables-scss.scss`). Its height is `$mobileTabNavHeight` (62 px).
-   Any page whose content can reach the bottom of the viewport must add `margin-bottom` or reduce its height by `$mobileTabNavHeight` inside a `@media screen and (max-width: vars.$mobileTabSwitchWidth)` block to prevent the fixed bar from overlapping content.

### Global Styles

-   Location: `projects/edu-sharing-ui/assets/scss`

### API Services

-   Location: `projects/edu-sharing-api`

### Options Helper

-   Location: `src/app/services/options-helper.service.ts`; individual option groups split into `src/app/services/options/` (`primary-options.ts`, `view-options.ts`, `reuse-options.ts`, `edit-options.ts`, `file-operations-options.ts`, `delete-options.ts`, `toggle-options.ts`).
-   Each factory file exports a single `createXxxOptions({ service, management, components, data }: OptionsContext)` function. `OptionsContext` (in `options-context.ts`) uses `import type { OptionsHelperService }` to avoid a circular runtime module dependency — do not change it to a value import.
-   Methods on `OptionsHelperService` called by factory files must be `protected` (e.g. `cutCopyNode`, `revokeNode`, `goToWorkspace`, `removeFromCollection`, `bookmarkNodes`, `unblockImportedNodes`); injected services accessed by factory files are `public`.
-   Provides declarative options configuration, e.g. for the `actionbar`
-   To bind a standalone `es-actionbar` to a node context (outside `es-node-entries-wrapper`): provide `OptionsHelperDataService` in the component's `providers`, use a `@ViewChild` **setter** (not field) to call `await initComponents(actionbar)` then `refreshComponents()` when the bar first enters the DOM (e.g. after `*ngIf` becomes true), and use an `effect()` to call `setData(…)` + `refreshComponents()` when the active node changes while the actionbar stays rendered. `OptionsHelperService` is already provided at `editorial-page` level — only `OptionsHelperDataService` needs to be added locally.
-   `OptionsHelperDataService.setData()` calls `wrapOptionCallbacks()`, which replaces each `option.callback` so that when the actionbar calls it with no arguments the node is resolved from `data.activeObjects`. The original callback therefore receives `(undefined, [activeNode])` — write callbacks as `(node, nodes) => fn(node ?? nodes?.[0])` so they work correctly from both `es-node-entries-wrapper` (passes node as first arg) and the standalone actionbar.
-   The `getDownloadOption(data)` callback closes over its `data` argument. When used with `OptionsHelperDataService`, pass a **shared `OptionData` object reference** and update `activeObjects` on it before each `setData` + `refreshComponents` call, so the download callback resolves the correct node via `getObjects(object, closureData)` even after wrapping.

### Connector Create Options

-   `ConnectorOptionsService` (`src/app/services/connector-options.service.ts`) is the single source for "create-with-connector" `OptionItem[]`. Use `observeConnectors()` for the filtered list (regular + `simpleConnectors`, merged through `RestConnectorsService.filterConnectors`) and `buildOptions(onSelect)` for the menu options observable. Do not re-implement this list inline (was duplicated in `create-menu` and `nodes-selector`).

### Editorial Sidebar

-   `EditorialSidebarService.applyNodeEmitted` (`providedIn: 'root'`) is the channel for "the sidebar produced these nodes for the host". Payload: `{ nodes: Node[]; parent?: Node; connectorId?: string; window?: Window }`. When `connectorId`/`window` are set, the new node was created via a connector and the host can hand them to `startConnectorPolling`-style write-back logic. Multiple components (e.g. `manage-assignment`, `submit-assignment`) subscribe to the same emitter.

### Notifications/Toasts/Snackbar

-   Short information (confirm/error) feedbacks should use the `toast.ts` service. Preferred language path for message keys is `TOAST.`.

### Dialogs Service

-   Location: `src/app/features/dialogs`
-   Global modal & generic dialog toolkit

### Internationalization (i18n)

-   Library: `@ngx-translate/core`
-   Translation files: `src/assets/i18n/`
-   Structure: grouped by component, then language (`de.json`, `en.json`, …)

### Tool Permissions

-   Frontend constants: `projects/edu-sharing-api/src/lib/rest-constants.ts` — add `TOOLPERMISSION_*` string constants here
-   Backend constants mirror: `Backend/alfresco/common/src/main/java/org/edu_sharing/repository/client/tools/CCConstants.java` — add matching `CCM_VALUE_TOOLPERMISSION_*` constant
-   Backend registration: `Backend/alfresco/module/src/main/java/org/edu_sharing/alfresco/service/toolpermission/ToolPermissionBaseService.java`
    -   Add to `getAllPredefinedToolPermissions()` to register the permission
    -   Add a `.remove()` call in `getAllDefaultAllowedToolpermissions()` to make it **disabled by default**
-   UI management: `src/app/pages/user-management-page/toolpermission-manager/toolpermission-manager.component.ts` — add to the appropriate group in `GROUPS`
-   Translations: add a `"TOOLPERMISSION_<NAME>"` key in each `src/assets/i18n/common/<lang>.json`
-   Template evaluation: use the `esToolpermission` pipe with `async`, e.g.:
    ```html
    *ngIf="('TOOLPERMISSION_FOO' | esToolpermission | async)"
    ```
