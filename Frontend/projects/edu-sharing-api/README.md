# Ngx Edu-Sharing Api

Angular bindings for Edu-Sharing's API.

The package includes the auto-generated `ApiModule`. However, exported services are custom wrappers,
that focus on getting information without having to worry about what requests are made in the
background. As a rule of thumb, users should be able to tell this library what they _want_ as
opposed to how to get it. This library should provide `Observable`s which update when appropriate.

Currently, this library is in an **incomplete** state and will be extended over time.

## Installation

Install inside your Angular project.

```sh
npm i ngx-edu-sharing-api
```

Import `EduSharingApiModule` in your app module:

```ts
@NgModule({
    imports: [EduSharingApiModule.forRoot()],
})
export class AppModule {}
```

### Using a Development Version of Ngx Edu-Sharing Api

-   Build the library as described in [Build](#build).
-   In `dist/edu-sharing-api`, run
    ```sh
    npm link
    ```
-   In the project directory where you want to use Ngx Edu-Sharing Api, run
    ```sh
    npm link npx-edu-sharing-api
    ```

You might need to add the following paths to the `tsconfig.json` of your project:

```json
{
    "compilerOptions": {
        "paths": {
            "rxjs": ["./node_modules/rxjs"],
            "rxjs/*": ["./node_modules/rxjs/*"],
            "@angular/*": ["./node_modules/@angular/*"]
        }
    }
}
```

Also, in your `angular.json` you will have to set

```json
{
    "projects": {
        "<your-project>": {
            "architect": {
                "build": {
                    "configurations": {
                        "development": {
                            "preserveSymlinks": true
                        }
                    }
                }
            }
        }
    }
}
```

## Configuration

| Parameter | Description                                  | Example                                                  |
| --------- | -------------------------------------------- | -------------------------------------------------------- |
| rootUrl   | The root URL to the REST API                 | `'https://my-edu-sharing-instance.com/edu-sharing/rest'` |
| onError   | Default error handler to call on HTTP errors | `(err) => console.error('oh, no!', err)`                 |

Either provide the configuration with `forRoot()`:

```ts
@NgModule({
    imports: [
        EduSharingApiModule.forRoot({ rootUrl: environment.eduSharingApiUrl }),
    ],
})
```

Or provide `EDU_SHARING_API_CONFIG` yourself, which allows you to use dependency injection:

```ts
@NgModule({
    providers: [
        {
            provide: EDU_SHARING_API_CONFIG,
            deps: [ErrorHandlerService],
            useFactory: (errorHandler: ErrorHandlerService) => ({
                onError: (err) => errorHandler.handleError(err),
            } as EduSharingApiConfigurationParams),
        },
    ],
})
```

## Usage

Import services and models from `ngx-edu-sharing-api`. Available services can be found in
`node_modules/ngx-edu-sharing-api/lib/wrappers/`. E.g.:

```ts
import { Injectable } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class EduSharingService {
    static readonly repository = 'local';

    constructor(private nodeService: NodeService) {}

    getNode(id: string): Observable<Node> {
        return this.nodeService.getNode(EduSharingService.repository, id);
    }
}
```

If not stated otherwise, methods will throw errors of type `ApiErrorResponse` (see [Error Handling](#error-handling)).

### `Observable`s

This library uses `Observable`s as return types. There are methods that

-   trigger a request and return the result with an `Observable`.  
    This is the behavior of most methods re-exported from `./lib/api/services` and some wrapped
    methods.  
    Names of these methods usually start wit `get` (except re-exported).  
    Example: `search.getPage()`.
-   trigger a request but yield no result.  
    This is the behavior of some methods re-exported from `./lib/api/services` and some wrapped
    methods.  
    Names of these methods usually don't have a prefix and are named as an action.  
    Example: `search.loadMoreFacets()`.  
    :warning: Do not forget to subscribe to the returned `Observable` anyway!
-   return a cached result with an `Observable`.  
    Names of these methods usually start wit `get`.  
    This is only done when we know it is safe to return old values. You can use these methods like
    normal API methods, but we safe some requests.  
    Example: `about.getAbout()`.
-   return a result each time its value changes.  
    Names of these methods start wit `observe`.  
    We provide these methods when we have a way of knowing when to refetch information. You can just
    subscribe for as long as you need updated information and not care about requests.  
    Example: `search.observeFacets()`.  
    :warning: Do not forget to unsubscribe when you don't need the values anymore!  
    :warning: These `Observable`s will never complete!

### Things to Keep in Mind When Using This Library

> `Observable`s returned by methods starting with `observe` emit more then once.

```ts
this.authenticationService.observeLoginInfo().subscribe((loginInfo) => {
    // This will be called multiple times!
});
```

```ts
// This will never resolve!
await this.authenticationService.observeLoginInfo().toPromise();
```

Use `first()` to get an observable that emits once and completes:

```ts
import { first } from 'rxjs/operators';

this.authenticationService
    .observeLoginInfo()
    .pipe(first())
    .subscribe((loginInfo) => {
        // This will be called only once.
    });
```

> Subscribe to observables even if you are not interested in the result.

```ts
this.searchService.loadMoreFacets(facet, size).subscribe();
```

> Do not alter objects returned by this library.

```ts
this.aboutService.getAbout().subscribe((about) => {
    // Don't to this!
    about.version.repository = getMajorVersion(about.version.repository);
});
```

Instead, create new objects and replace properties:

```ts
this.aboutService.getAbout().subscribe((about) => {
    const aboutCopy = {
        ...about,
        version: {
            ...about.version,
            repository: getMajorVersion(about.version.repository),
        },
    };
});
```

> Do not forget to unsubscribe from `Observable`s returned by methods starting with `observe` when
> using inside components or locally scoped services.

```ts
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { FacetsDict, SearchService } from 'ngx-edu-sharing-api';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'app-foo',
    templateUrl: './foo.component.html',
})
export class FooComponent implements OnInit, OnDestroy {
    facets: FacetsDict;
    private readonly destroyed$ = new Subject<void>();

    constructor(private searchService: SearchService) {}

    ngOnInit(): void {
        this.searchService
            .observeFacets(['ccm:foo', 'ccm:bar'])
            // Unsubscribe when the component is destroyed.
            .pipe(takeUntil(this.destroyed$))
            .subscribe((facets) => (this.facets = facets));
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
```

### Error Handling

This library allows to define a default error handler `onError`, that will be called on all HTTP
errors (see [Configuration](#configuration)).

You can choose to prevent this for individual calls by catching the error and calling
`preventDefault()` on it:

```ts
this.searchApi
    .search({
        /* ... */
    })
    .subscribe({
        next: (results) => {
            /* handle results */
        },
        error: (err: ApiErrorResponse) => {
            /* handle error in a way that makes the default error handler obsolete */
            err.preventDefault();
        },
    });
```

You can also choose to do additional error handling and also run the default error handler by not
calling `preventDefault()`. In this case, the default error handler is run _after_ the subscribed
one.

## Maintaining Ngx Edu-Sharing Api

### Adding New Methods

The preferred way of adding methods is to provide wrappers that care about caching and updating
values.

When wrappers would not provide much benefit, you can also re-export methods from
`./lib/api/services` in `public-api.ts`. When doing this, take care not to expose any API endpoints
which other wrappers rely on controlling themselves for consistent state.

### Update And Generate Edu-Sharing API Code

Download an updated `swagger.json` to `build`, e.g.:

```sh
wget https://redaktion-staging.openeduhub.net/edu-sharing/rest/openapi.json -O build/openapi.json
```

Generate API Code:

```sh
npm run generate-api
```

### Windows Quirks

Configure your Git to keep line endings to prevent changes to unmodified files:

```sh
git config core.autocrlf input
```

Run the `npm run generate-api` inside a WSL shell, otherwise the prettier might fail.

### Code scaffolding

Run `ng generate component component-name --project edu-sharing-api` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module --project edu-sharing-api`.

> Note: Don't forget to add `--project edu-sharing-api` or else it will be added to the default project in your `angular.json` file.

### Build

Run `ng build edu-sharing-api` to build the project. The build artifacts will be stored in the `dist/` directory.

### Publishing

After building your library with `ng build edu-sharing-api`, go to the dist folder `cd dist/edu-sharing-api` and run `npm publish`.

### Running unit tests

Run `ng test edu-sharing-api` to execute the unit tests via [Karma](https://karma-runner.github.io).

### Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
