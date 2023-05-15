# Angular edu-sharing

This folder contains the frontend part of edu-sharing.

The frontend uses Angular and TypeScript and builds via [angular-cli](https://github.com/angular/angular-cli).

To get more help on the `angular-cli` use `ng help` or go check out the [Angular-CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

## Setup Development Environment

```sh
git submodule update --init
npm install
```

## Configuration

```sh
cp .env.example .env
```

Then edit `.env` to configure the environment. At least, you need `BACKEND_URL` to point to a running edu-sharing instance.

## Development server

Run `npm start` for a dev server. Navigate to `http://localhost:4200/edu-sharing`. The app will automatically reload if you change any of the source files.

## API

API-specific code is maintained in `projects/edu-sharing-api` and compiled separately.

To use new or updated API endpoints, first make sure `../Backend/services/rest/api/src/main/resources/openapi.json` is up to date:

```sh
curl \
    -H 'Accept: application/json' \
    --user 'admin:admin' \
    http://repository.127.0.0.1.nip.io:8100/edu-sharing/rest/openapi.json \
    | jq -S . \
    > ../Backend/services/rest/api/src/main/resources/openapi.json
```

Then either directly export your endpoint service in `projects/edu-sharing-api/src/public-api.ts` or provide a wrapper in `projects/edu-sharing-api/src/lib/wrappers` and export that. Data models are exported in `projects/edu-sharing-api/src/lib/models.ts`.

Run `npm start` to update the `edu-sharing-api` project.

Always import services and models via `ngx-edu-sharing-api`, e.g.,

```ts
import { ConfigService } from 'ngx-edu-sharing-api';
```

For more information, see [projects/edu-sharing-api/README.md](projects/edu-sharing-api/README.md).

## Running Unit Tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running End-to-end Tests

Configure the test setup via `.env`.
If you test against `localhost:4200`, make sure you are serving the app via `ng serve`.
Run all tests using the command `npm run e2e`.

For more information, see [tests/README.md](tests/README.md).

## Development

### Managing routes

Routes are defined in the file `/src/app/router.component.ts`.

### Managing Language

Open the specific components langauge file, you can find them in `src/assets/i18n/<component>/<language>.json`.
