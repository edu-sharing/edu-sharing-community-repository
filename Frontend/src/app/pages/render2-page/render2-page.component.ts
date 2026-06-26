import { Component, Input, OnDestroy, signal, ViewChild } from '@angular/core';
import { LocalEventsService, OptionsHelperDataService } from 'ngx-edu-sharing-ui';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, Subject } from 'rxjs';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { CommonModule, Location, PlatformLocation } from '@angular/common';
import { RenderWrapperComponent } from './render-wrapper-component/render-wrapper.component';
import { RestConstants } from 'ngx-edu-sharing-api';
import { takeUntil } from 'rxjs/operators';
import { AppComponent } from '../../app.component';
import { ConfigurationService } from '../../core-module/core.module';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { RouterHelper } from '../../util/router.helper';

@Component({
    selector: 'es-render2-page',
    templateUrl: 'render2-page.component.html',
    styleUrls: ['render2-page.component.scss'],
    imports: [CommonModule, RenderWrapperComponent],
    providers: [OptionsHelperDataService],
})
export class Render2PageComponent implements OnDestroy {
    private readonly destroyed$ = new Subject<void>();
    @Input() nodeId = signal<string>(null);
    @Input() repository = signal<string>(null);
    @Input() childId = signal<string>(null);
    @ViewChild(RenderWrapperComponent) renderWrapper: RenderWrapperComponent;
    version = signal<string>(null);
    private closeOnBack = false;
    private fromLogin = false;
    private isDestroyed = false;
    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private location: Location,
        private mainNav: MainNavService,
        private localEvents: LocalEventsService,
        private platformLocation: PlatformLocation,
        private configService: ConfigurationService,
    ) {
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
                this.closeOnBack = queryParams.closeOnBack === 'true';
                this.fromLogin =
                    queryParams.fromLogin === 'true' || queryParams.redirectFromSSO === 'true';
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
        this.isDestroyed = true;
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

    close() {
        if (this.closeOnBack) {
            window.close();
            return;
        }
        if (this.fromLogin && !AppComponent.isRedirectedFromLogin()) {
            UIHelper.goToDefaultLocation(
                this.router,
                this.platformLocation,
                this.configService,
                false,
            );
            return;
        }
        this.location.back();
        // use a timeout to let the browser try to go back in history first
        setTimeout(() => {
            if (this.isDestroyed) {
                return;
            }
            if (AppComponent.history.value?.length > 1) {
                const last = AppComponent.history.value[AppComponent.history.value.length - 2];
                console.info('Enforcing back, may h5p navigation was present');
                RouterHelper.navigateToAbsoluteUrl(this.platformLocation, this.router, last, true);
                return;
            }
            this.mainNav.patchMainNavConfig({ showNavigation: true });
            setTimeout(() => {
                this.mainNav.getMainNav().topBar?.toggleMenuSidebar();
                this.mainNav
                    .getMainNav()
                    .topBar.closeScopeSelector.pipe(takeUntil(this.destroyed$))
                    .subscribe(() => {
                        this.mainNav.patchMainNavConfig({ showNavigation: false });
                    });
            });
        }, 250);
    }

    private refresh() {
        void this.renderWrapper?.refresh();
    }
}
