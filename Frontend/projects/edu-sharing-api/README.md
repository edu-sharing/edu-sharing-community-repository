# Ngx Edu-Sharing Api

Angular bindings for Edu-Sharing's API.

The package includes the auto-generated `ApiModule`. However, exported services are custom wrappers,
that focus on getting information without having to worry about what requests are made in the
background. As a rule of thumb, users should be able to tell this library what they _want_ as
opposed to how to get it. This library should provide `Observable`s which update when appropriate.

## Update And Generate Edu-Sharing API Code

Download an updated `swagger.json` to `build`, e.g.:

```sh
wget https://redaktion-staging.openeduhub.net/edu-sharing/rest/openapi.json -O build/openapi.json
```

Generate API Code:

```sh
npm run generate-api
```

## Code scaffolding

Run `ng generate component component-name --project edu-sharing-api` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module --project edu-sharing-api`.

> Note: Don't forget to add `--project edu-sharing-api` or else it will be added to the default project in your `angular.json` file.

## Build

Run `ng build edu-sharing-api` to build the project. The build artifacts will be stored in the `dist/` directory.

## Publishing

After building your library with `ng build edu-sharing-api`, go to the dist folder `cd dist/edu-sharing-api` and run `npm publish`.

## Running unit tests

Run `ng test edu-sharing-api` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
