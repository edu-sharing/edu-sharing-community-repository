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

## Key Areas

### Mds (Metadatasets)

-   Location: `src/app/features/mds` and `src/app/features/mds/mds-editor/widgets`
-   Handles widget definitions and dynamic frontend templating driven by metadata configurations

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
