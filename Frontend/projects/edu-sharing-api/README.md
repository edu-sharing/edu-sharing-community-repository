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
    imports: [
        EduSharingApiModule.forRoot({ rootUrl: environment.eduSharingApiUrl }),
        // E.g.: rootUrl: 'https://my-edu-sharing-instance.com/edu-sharing/rest
    ],
})
export class AppModule {}
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

Do not forget to unsubscribe from `Observable`s when using inside components or locally scoped
services:

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
            .getFacets(['ccm:foo', 'ccm:bar'])
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

## Maintaining Ngx Edu-Sharing Api

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
