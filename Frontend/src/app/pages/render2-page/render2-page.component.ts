import { Component, Input, OnDestroy, signal, ViewChild, inject } from '@angular/core';
import { LocalEventsService, OptionsHelperDataService } from 'ngx-edu-sharing-ui';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, Subject } from 'rxjs';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { Location } from '@angular/common';
import { RenderWrapperComponent } from './render-wrapper-component/render-wrapper.component';
import { RestConstants } from 'ngx-edu-sharing-api';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'es-render2-page',
    templateUrl: 'render2-page.component.html',
    styleUrls: ['render2-page.component.scss'],
    imports: [RenderWrapperComponent],
    providers: [OptionsHelperDataService],
})
export class Render2PageComponent implements OnDestroy {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private location = inject(Location);
    private mainNav = inject(MainNavService);
    private localEvents = inject(LocalEventsService);

    private readonly destroyed$ = new Subject<void>();
    @Input() nodeId = signal<string>(null);
    @Input() repository = signal<string>(null);
    @Input() childId = signal<string>(null);
    @ViewChild(RenderWrapperComponent) renderWrapper: RenderWrapperComponent;
    version = signal<string>(null);
    constructor() {
        this.mainNav.setMainNavConfig({
            show: true,
            showNavigation: false,
            currentScope: 'render',
        });
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeId.set(params.node);
                this.childId.set(queryParams.childobject_id || null);
                this.repository.set(queryParams.repository || queryParams.repo || null);
                this.version.set(params.version || RestConstants.NODE_VERSION_CURRENT);
            },
        );

        this.localEvents.nodesChanged
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.refresh());
        this.localEvents.nodesDeleted
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.close());
    }
    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
    setChildId(childobject_id: string) {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParamsHandling: 'merge',
            queryParams: {
                childobject_id,
            },
            replaceUrl: true,
        });
    }

    private close() {
        this.location.back();
    }

    private refresh() {
        void this.renderWrapper?.refresh();
    }
}
