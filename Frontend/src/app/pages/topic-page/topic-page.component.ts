import { Component, signal, ViewChild, WritableSignal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { TemplateComponent } from './editor/template.component';

@Component({
    selector: 'es-custom-topic-page',
    templateUrl: './topic-page.component.html',
    styleUrls: ['./topic-page.component.scss'],
    standalone: false,
})
export class TopicPageComponent {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private translate = inject(TranslateService);

    // defaults to the main collection of physics
    topicCollectionId: WritableSignal<string> = signal(null);
    topicVariantId: WritableSignal<string> = signal('');
    topicPageLoaded: WritableSignal<boolean> = signal(false);

    @ViewChild('templateComponent') templateComponent: TemplateComponent;

    constructor() {
        this.route.queryParams
            .pipe(takeUntilDestroyed())
            .subscribe(async (params: Params): Promise<void> => {
                if (!this.topicPageLoaded()) {
                    if (params.collectionId) {
                        this.topicCollectionId.set(params.collectionId);
                    }
                    if (params.variantId) {
                        this.topicVariantId.set(params.variantId);
                    }
                    this.topicPageLoaded.set(true);
                } else {
                    if (params.openMenu) {
                        switch (params.openMenu) {
                            case 'profiling': {
                                this.templateComponent?.collapsibleItemClicked(
                                    this.translate.instant('TOPIC_PAGE.SIDE_MENU.PROFILING.LABEL'),
                                );
                                break;
                            }
                            case 'topicTree': {
                                this.templateComponent?.collapsibleItemClicked(
                                    this.translate.instant('TOPIC_PAGE.SIDE_MENU.TOPIC_TREE.LABEL'),
                                );
                                break;
                            }
                            case 'statistics': {
                                this.templateComponent?.collapsibleItemClicked(
                                    this.translate.instant('TOPIC_PAGE.SIDE_MENU.STATISTICS.LABEL'),
                                );
                                break;
                            }
                            case 'settings': {
                                this.templateComponent?.collapsibleItemClicked(
                                    this.translate.instant(
                                        'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.LABEL',
                                    ),
                                );
                                break;
                            }
                            default: {
                                return;
                            }
                        }
                        // create params without openMenu
                        const queryParams = { ...params };
                        delete queryParams.openMenu;

                        // update route with new params
                        await this.router.navigate([], {
                            relativeTo: this.route,
                            queryParams: queryParams,
                            queryParamsHandling: '',
                        });
                    }
                }
            });
    }
}
