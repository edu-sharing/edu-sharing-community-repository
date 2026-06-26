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

-   **Framework**: Angular (with `@angular/material` / `@angular/cdk` v21)
-   **Language**: TypeScript
-   **Test runner**: Jest / Playwright
-   **Styling**: SCSS
-   **Local libs path mapping differs by tsconfig**: `tsconfig.app.json` maps `ngx-edu-sharing-api`/`ngx-edu-sharing-ui` to `projects/*/src/lib` (the **source**), so `ng build`/`serve` picks up library edits directly. The root `tsconfig.json` maps them to `dist/*`, which the IDE language service may use — so after adding a new lib `@Input()`/export you can get stale "property not provided / not exported" errors in the editor that do **not** reflect the actual build. Rebuild the lib (or ignore) to clear them.

## Linting & Styles

-   Obey the tslint rules
-   if, loops and other containers should always have braces

## Key Areas

### Mds (Metadatasets)

-   Location: `src/app/features/mds` and `src/app/features/mds/mds-editor/widgets`
-   Handles widget definitions and dynamic frontend templating driven by metadata configurations
-   **Native widgets** (`author`, `license`, `preview`, …; enum `NativeWidgetType`, registry `NativeWidgets` in `mds-editor/types/mds-types.ts`) are Angular components. They are only registered into `MdsEditorInstanceService.nativeWidgets` when the editor **view is rendered** (`MdsEditorViewComponent.injectNativeWidget` → `registerNativeWidget`). `initWithNodes()` alone builds the model layer (`this.widgets`, including a `Widget` for each native tag with `definition.isRequired` parsed from the `required`/`isrequired` attribute — see `parse-attributes.ts` mapping `required → isRequired`) but does **not** instantiate components. So flows that call `initWithNodes` without rendering the editor (e.g. the **publish dialog**, `share-dialog/publish/publish.component.ts`) have an empty `nativeWidgets`.
-   **Do NOT import `NativeWidgets` (from `types/mds-types`) into `mds-editor-instance.service.ts`.** It creates a circular import (`instance.service → mds-types → *-widget.component → instance.service`) that leaves registry entries `undefined` at runtime — symptom: native widgets render as regular widgets and throw _"Widget for type undefined is not implemented"_ (the `author` entry is the one that goes `undefined`; the editor masks it via the `getCustomNativeWidgets()` fallback in `findNativeWidget`). Import aliasing does **not** fix this (same module edge). That's why the existing graphql path references `NativeWidgets` without importing it.
-   For per-native-type logic the instance service needs (e.g. emptiness for the completion status / `observeCompletionStatus()` when the widget isn't rendered), put it in a **leaf util** that imports only `RestConstants`/`VCard`/`types` — see `mds-editor/util/native-widget-completion.ts` (`authorIsEmpty`, `nativeWidgetEmptyCheckers`). Both the native widget component and the instance service import it cycle-free.
-   `MdsEditorInstanceService`: `values$` is populated by `initWithValues`/`updateNodes`, but **not** by `initWithNodes` (which only sets `nodes$`). A node's `properties` is itself a `Values` map — code needing current values from either init path should use `nodes$.value[0]?.properties ?? values$.value`.
-   **Widget-type → component registry** is `WidgetComponents` (and `NativeWidgets`) in `mds-editor/types/mds-types.ts`. Several `MdsWidgetType`s map to the **same** component (e.g. `multivalueSuggestBadges`/`multivalueFixedBadges`/`*Tree` → `MdsEditorWidgetTreeComponent`), which then branches on `widget.definition.type` internally — so per-type behavior usually lives inside one shared component, not in separate components. The `MdsWidgetType` enum itself lives in `projects/edu-sharing-ui/.../mds-viewer/widget/mds-widget.component.ts` (lib), not in the app's mds feature. `MdsEditorWidgetChipsComponent` is `@Deprecated` (superseded by the tree component) but `multivalueBadges`/`singlevalueSuggestBadges` still route to it.
-   **Widget suggestion/autocomplete values** come from `Widget.getSuggestedValues()` (`mds-editor-instance.service.ts`): if the widget has a local valuespace (`definition.values`) it filters those client-side; otherwise it makes a **remote** call via the deprecated `RestMdsService.getValues()` (POST `.../metadatasets/{set}/values`). Remote suggestions require ≥2 chars and (in `search` mode) include the other widgets' values as criteria.
-   **`EditorMode`** (`types/types.ts`) is the cross-cutting flag that changes widget behavior/layout (`nodes`/`search`/`form`/`inline`/`viewer`/`valueSelection`). Components gate template branches on it (e.g. `editorMode === 'valueSelection'`); the same flag governs bulk support, whether all-vs-changed values are returned, and required-validation.

#### Storybook for MDS

-   `mds-editor/storybook-utils.ts` bundles its **own** full MDS definition (a mirror of `config/defaults/.../mds.xml`) plus the provider mock list `mdsStorybookProviders`. The storybook MDS is independent of the live config — to make a widget/view appear in a story, edit the view's `html` in `storybook-utils.ts`, not the real `mds.xml`.
-   `mds-editor-wrapper` stories use the **real** `MdsEditorInstanceService` (via `MdsEditorInstanceServiceMock`) and real `Widget` instances, so any backend service a widget calls (e.g. `RestMdsService`, `MdsService`, `SuggestionsV1Service`) must be mocked in `mdsStorybookProviders` or the real HTTP call fires and fails. Widget-level stories instead use the standalone `WidgetDummy` which stubs methods like `getSuggestedValues` directly.

### General notes

-   use custom `esIcon` directive instead of `mat-icon`
-   Prefer signal view queries (`viewChild()` / `viewChildren()` + `computed()`) over the legacy `@ViewChild` setter-with-`setTimeout` workaround for "ExpressionChangedAfterItHasBeenChecked" — signal queries settle cleanly and can be combined/filtered in a `computed()` that templates read directly (Angular 21).
-   A component reused across **query/route param changes** (e.g. tabs or `repo` query param) does **not** re-run its constructor/`ngOnInit`; subscribe to the param/value observable to react. Pages also share state via `providedIn:'root'` / page-level `BehaviorSubject`s (e.g. `SearchPageService.selection` written by multiple sibling result components) — a newly-routed instance inherits the **stale** value, so reset such shared subjects explicitly on init rather than relying on the new instance's empty local state.
-   Connector display names are translated via the `CONNECTOR.<id>.NAME` i18n key, where `<id>` is the connector id (e.g. `ONLYOFFICE`) from `RestConnectorsService.connectorSupportsEdit(node)?.id`. Translate it in the template, not in TS.
-   To override `mat-button` label color in a dark/overlay context, use the MDC token `--mdc-text-button-label-text-color` (e.g. `button.mat-mdc-button { --mdc-text-button-label-text-color: #fff; }`). The project uses `angular-material-css-vars.
-   `NodeHelperService.getOriginalId(node)` (in `src/app/services/node-helper.service.ts`) returns the `ccm:original` property value when present, falling back to `node.ref.id`. Use it to detect collection references and submission file variant content nodes.
-   Do NOT use spread syntax (`[...x]`, `{...x}`) in templates — it is only supported in Angular 21.1+ and this project is on an older version. A `Set`/`Map` won't serialize with the `json` pipe either (renders `{}`); convert to an array in the component if you need to inspect it.
-   `UIService.editConnector(node, type?, win?, connectorType?)` and `openConnector(...)` return the opened `Window`. Pre-open the window in the user-gesture (synchronously) and pass it in if the connector call follows an `await`, to avoid popup blockers.
-   Node list overlays: `es-node-entries-wrapper` projects an `<ng-template #overlay let-element="element">` (via `@ContentChild('overlay')`) rendered per card for `Grid`/`SmallGrid` display types. The card's `.card-overlay` provides the positioning context.
-   `es-node-entries-wrapper` (table view) hides built-in columns via `[checkbox]="false"`, `[showIconColumn]="false"`, `[showActions]="false"`. These flow wrapper → `NodeEntriesService` (`BehaviorSubject`s) → `node-entries-table` `getVisibleColumnNames()`, which builds `select`/`icon`/data-columns/`actions`. Add new column toggles using this same pattern.
-   Prefer the RxJS observer-object form `subscribe({ next, error })`; the positional `subscribe(next, error)` overload is deprecated (TS6385) and triggers lint warnings.
-   The top navigation bar is a single global `<es-main-nav>` in `app.component.html` driven by `MainNavService`. Pages do **not** render their own nav element — they call `mainNav.setMainNavConfig({ title, currentScope, … })` in `ngOnInit` (only `currentScope` is required). The bar's height is the `--mainnavHeight` CSS var.
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
-   **SCSS namespace gotcha**: `@use './common' as *` only exposes common's mixins — it does **not** re-export `variables-scss` (common `@use`s it, not `@forward`). To use SCSS vars like `$backgroundColor` / `$mobileTabSwitchWidth` / `$mobileTabNavHeight` in a component stylesheet, add your own `@use 'projects/edu-sharing-ui/assets/scss/variables-scss' as vars;` and reference `vars.$x` (bare `$x` will fail to compile).
-   Reusable mixins live in `projects/edu-sharing-ui/assets/scss/mixins.scss`: `materialShadow()` / `materialShadowSmall()`, and `materialScrollbar()` / `materialScrollbarLight()` for styled scrollbars on internal scroll areas.
-   **Rounded container card**: `roundedContainerCard()` (frame — margin / `--roundedContainerBackground` / `--borderRadiusCards` / shadow + mobile bottom-nav `margin-bottom` clearance) and `roundedContainerStaticScroll()` (makes it a static card that scrolls its content internally: `display:flex; flex-direction:column; overflow:hidden` + `height: calc(100vh - 2×--roundedContainerMargin - --mainnavCurrentHeight)` with the `$mobileTabNavHeight` subtraction). Pair with a scrolling child `flex:1; min-height:0; overflow-y:auto`. Used by both editorial-page and search-page `.main-content`.

### Dark Mode / Theming

-   **The darkmode toggle mechanism**: dark mode is toggled by a single class **`body.isDarkTheme`** on `document.body`, added by `angular-material-css-vars` when `ThemeService` calls `materialCssVarsService.setDarkTheme(isDark)` (`src/app/services/theme.service.ts`, driven by `registerDarkMode()`). Nothing is recompiled on toggle — only this class changes, and the CSS-variable cascade does the rest. `ThemeService.isDarkMode` is a signal you can read in templates (e.g. logo swap: `isDarkMode() ? 'assets/images/logo_darkmode.svg' : 'assets/images/logo.svg'`).
-   **Where the variables live**: `projects/edu-sharing-ui/assets/scss/variables.scss` declares light values in `:root { … }` and **redefines the same custom properties under `body.isDarkTheme { … }`** for dark. CSS custom properties inherit, so the dark block overrides `:root` for the whole body subtree, live. Prefer deriving dark values from the Material palette via `color-mix(in srgb, var(--primary), …)` so they follow palette colour swaps automatically rather than being fixed hex.
-   **Dont hardcode colour literals** (hex/`rgb()`/named colours) in component SCSS or templates. Every colour should be a global custom property defined in `variables.scss` — a light value in `:root`, plus a dark override in `body.isDarkTheme` when it needs to flip. A hardcoded colour will not respond to dark mode and is a bug. To colour a component, reference `var(--…)`; if no suitable variable exists, **add one to `variables.scss` (both blocks)** rather than inlining a value.
-   **Be careful using the raw `--palette-primary-X` variables directly** (e.g. `--palette-primary-50`/`-900`). They are **not** redefined in the `body.isDarkTheme` block, so they keep their fixed light→dark tint ordering and **do not flip** — a `--palette-primary-50` used as a surface stays a light tint and reads wrong on a dark background. Use the `--primaryBackgroundXX` aliases instead (`--primaryBackground50/100/300/700/900`): they equal the matching palette swatch in `:root` but **are** overridden under `body.isDarkTheme` (via `color-mix(in srgb, var(--primary), black/white)`), so they flip correctly with the theme.
-   **SCSS `$variables` can't flip with dark mode** — they're resolved at compile time. Only CSS custom properties (`var(--…)`) react to `body.isDarkTheme`, so a colour SCSS var must hold a `var(--…)` value (`$surface: var(--cardLightBackground)`), not a literal.

### API Services

-   Location: `projects/edu-sharing-api`
-   **Never use the `@Deprecated` legacy `RestXxxService` from `src/app/core-module/rest/services/`** (e.g. `RestNodeService`, `RestStatisticsService`, `RestAdminService`). Use the `ngx-edu-sharing-api` equivalents — wrappers (`NodeService`, `OrganizationService`, …) or generated `*V1Service` (`StatisticV1Service`, `OrganizationV1Service`, …). Same for the DTOs in `core-module/rest/data-object.ts` (also slated for replacement).
-   Generated `*V1Service` methods take a **single params object** (e.g. `getStatisticsNode({ dateFrom, dateTo, grouping, … })`); request body goes in a `body` field. Date params are inconsistent across endpoints — some take **epoch millis** (`getStatisticsNode`), others **ISO-8601 strings** (`getByNodes`/`getByUsers`/`getByOrganization`); check the `fn/.../<op>.ts` `$Params` interface.
-   Only services and a subset of models are re-exported from the package public-api. The generated `$Params` interfaces and many models (e.g. `TrackingNode`, `Tracking`) are **not** exported — reference shapes structurally, define a local union/alias, or cast (`as unknown as`) at the boundary instead of importing them.
-   `NodeService.getNode(id)` (wrapper) already requests `propertyFilter: ['-all-']`, i.e. it hits `/node/v1/.../metadata` and returns all properties.
-   `core-module`'s `RestConstants extends RestConstantsBase` from `ngx-edu-sharing-api`; add new `TOOLPERMISSION_*`/string constants to `projects/edu-sharing-api/src/lib/rest-constants.ts` and they are available through `core-module`'s `RestConstants` too.
-   The `ngx-edu-sharing-api` client (`api/fn/<tag>/<op>.ts` + `api/services/*V1Service`) is generated by **ng-openapi-gen** from the backend OpenAPI (files are headed `Code generated … DO NOT EDIT`). After adding/changing a backend REST endpoint, regenerate the client (or, to keep things compiling meanwhile, hand-add the matching `fn` file + service method mirroring an existing operation — it will be reproduced identically on regen).

### Authoring `ngx-edu-sharing-ui` library code

-   Any declarable (component/directive/**pipe**) that a published `NgModule` in the lib **declares + exports** must also be re-exported from the library entrypoint `projects/edu-sharing-ui/src/lib/index.ts` (re-exported by `public-api.ts`), otherwise the lib build fails with **NG3001** "Unsupported private class … not exported from the top-level library entrypoint". Pipes live in `projects/edu-sharing-ui/src/lib/pipes/` and are declared/exported in `common/edu-sharing-ui-common.module.ts`.
-   Auth-state template gates use pipes over `AuthenticationService.observeLoginInfo()`: `esToolpermission` (`login.toolPermissions.includes(...)`) and `esGlobalAdmin` (`login.isAdmin`), both used with `async`. `WorkspaceExplorerComponent.getColumns(connector)` is a reusable static returning the standard node-column `ListItem[]` for column pickers.

### Options Helper

-   Location: `src/app/services/options-helper.service.ts`; individual option groups split into `src/app/services/options/` (`primary-options.ts`, `view-options.ts`, `reuse-options.ts`, `edit-options.ts`, `file-operations-options.ts`, `delete-options.ts`, `toggle-options.ts`).
-   Each factory file exports a single `createXxxOptions({ service, management, components, data }: OptionsContext)` function. `OptionsContext` (in `options-context.ts`) uses `import type { OptionsHelperService }` to avoid a circular runtime module dependency — do not change it to a value import.
-   Methods on `OptionsHelperService` called by factory files must be `protected` (e.g. `cutCopyNode`, `revokeNode`, `goToWorkspace`, `removeFromCollection`, `bookmarkNodes`, `unblockImportedNodes`); injected services accessed by factory files are `public`.
-   Provides declarative options configuration, e.g. for the `actionbar`
-   To bind a standalone `es-actionbar` to a node context (outside `es-node-entries-wrapper`): provide `OptionsHelperDataService` in the component's `providers`, use a `@ViewChild` **setter** (not field) to call `await initComponents(actionbar)` then `refreshComponents()` when the bar first enters the DOM (e.g. after `*ngIf` becomes true), and use an `effect()` to call `setData(…)` + `refreshComponents()` when the active node changes while the actionbar stays rendered. `OptionsHelperService` is already provided at `editorial-page` level — only `OptionsHelperDataService` needs to be added locally.
-   `OptionsHelperDataService.setData()` calls `wrapOptionCallbacks()`, which replaces each `option.callback` so that when the actionbar calls it with no arguments the node is resolved from `data.activeObjects`. The original callback therefore receives `(undefined, [activeNode])` — write callbacks as `(node, nodes) => fn(node ?? nodes?.[0])` so they work correctly from both `es-node-entries-wrapper` (passes node as first arg) and the standalone actionbar.
-   The `getDownloadOption(data)` callback closes over its `data` argument. When used with `OptionsHelperDataService`, pass a **shared `OptionData` object reference** and update `activeObjects` on it before each `setData` + `refreshComponents` call, so the download callback resolves the correct node via `getObjects(object, closureData)` even after wrapping.
-   `ListOptionsConfig.actionbar` (and `OptionsHelperComponents.actionbar`) accept a single `ActionbarComponent` **or an array** — `refreshComponents` computes the options once and assigns them to every bar, so multiple actionbars stay in sync with one selection/options computation (e.g. a toggles-only bar plus an actions-only bar). `ActionbarComponent` has `showToggleOptions` / `showActionOptions` inputs (default `true`) to render only the toggle or only the action options of the same options set; its `actionToggleDivider` should be gated on `showActionOptions` to avoid a dangling divider when actions aren't painted.

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

### Authentication State — `AuthenticationService` / `UserService`

-   **`AuthenticationService.createUserChanges()`** detects login-state changes by tracking both `authorityName` **and** `isValidLogin`. This is necessary because the backend may return the real `authorityName` before authentication is fully complete (e.g. during multi-step flows), so a change in `authorityName` alone is not a reliable signal that the user is now logged in.
-   **`UserService.createCurrentUser()`** merges `observeUserChanges()` with `observeLoginInfo().pipe(distinctUntilKeyChanged('isValidLogin'))` so that a flip of `isValidLogin` (e.g. 2FA completion) also triggers a user re-fetch — not just an `authorityName` change.
-   The `switchMap(() => observeLoginInfo().pipe(take(1)))` inside `createCurrentUser` normalises heterogeneous trigger types (void / LoginInfo) into the current `LoginInfo` for the downstream `switchReplay`.

### Tool Permissions

-   Frontend constants: `projects/edu-sharing-api/src/lib/rest-constants.ts` — add `TOOLPERMISSION_*` string constants here
-   Backend constants mirror: `Backend/alfresco/common/src/main/java/org/edu_sharing/repository/client/tools/CCConstants.java` — add matching `CCM_VALUE_TOOLPERMISSION_*` constant
-   Backend registration: `Backend/alfresco/module/src/main/java/org/edu_sharing/alfresco/service/toolpermission/ToolPermissionBaseService.java`
    -   Add to `getAllPredefinedToolPermissions()` to register the permission
    -   Add a `.remove()` call in `getAllDefaultAllowedToolpermissions()` to make it **disabled by default**
-   UI management: `src/app/pages/user-management-page/toolpermission-manager/toolpermission-manager.component.ts` — add to the appropriate group in `GROUPS`
-   Translations: add a `"TOOLPERMISSION_<NAME>"` entry **inside the nested `"TOOLPERMISSION": { … }` object** in each `src/assets/i18n/common/<lang>.json` (de/en/it/fr; `de-no-binnen-i.json` is a partial override that falls back to `de.json`). It is shown in the UI via the key `TOOLPERMISSION.<NAME>` — a missing entry renders that raw string in the toolpermission manager.
-   Template evaluation: use the `esToolpermission` pipe with `async`, e.g.:
    ```html
    *ngIf="('TOOLPERMISSION_FOO' | esToolpermission | async)"
    ```
