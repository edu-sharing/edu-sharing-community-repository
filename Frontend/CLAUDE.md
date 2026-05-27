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
-   Do NOT use spread syntax (`[...x]`, `{...x}`) in templates — it is only supported in Angular 21.1+ and this project is on an older version. A `Set`/`Map` won't serialize with the `json` pipe either (renders `{}`); convert to an array in the component if you need to inspect it.
-   `UIService.editConnector(node, type?, win?, connectorType?)` and `openConnector(...)` return the opened `Window`. Pre-open the window in the user-gesture (synchronously) and pass it in if the connector call follows an `await`, to avoid popup blockers.
-   Node list overlays: `es-node-entries-wrapper` projects an `<ng-template #overlay let-element="element">` (via `@ContentChild('overlay')`) rendered per card for `Grid`/`SmallGrid` display types. The card's `.card-overlay` provides the positioning context.

### Global Styles

-   Location: `projects/edu-sharing-ui/assets/scss`

### API Services

-   Location: `projects/edu-sharing-api`

### Options Helper

-   Location: `src/app/services/options-helper.service.ts`
-   Provides declarative options configuration, e.g. for the `actionbar`

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
