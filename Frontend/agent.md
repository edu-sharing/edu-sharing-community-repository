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
