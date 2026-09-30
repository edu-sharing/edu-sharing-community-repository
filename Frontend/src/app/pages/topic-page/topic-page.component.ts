import {
    Component,
    effect,
    ElementRef,
    signal,
    ViewChild,
    viewChild,
    WritableSignal,
    inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { EditorialSidebarService } from '../../features/editorial-sidebar/editorial-sidebar.service';
import { TopicPageGlobalService } from './shared/services/topic-page-global.service';
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
    private topicPageGlobalService = inject(TopicPageGlobalService);
    protected editorialSidebarService = inject(EditorialSidebarService);

    /** Component of the extension column beside the page, `null` while none is registered. */
    protected readonly customSidebarExtension =
        this.topicPageGlobalService.getCustomSidebarExtension();

    // defaults to the main collection of physics
    topicCollectionId: WritableSignal<string> = signal(null);
    topicVariantId: WritableSignal<string> = signal('');
    topicPageLoaded: WritableSignal<boolean> = signal(false);

    @ViewChild('templateComponent') templateComponent: TemplateComponent;
    protected readonly templatePage = viewChild<TemplateComponent>('templateComponent');
    private readonly sidebarColumn = viewChild<ElementRef<HTMLElement>>('sidebarColumnRef');
    private readonly previewColumn = viewChild('previewColumnRef', {
        read: ElementRef<HTMLElement>,
    });
    private readonly host = inject(ElementRef<HTMLElement>);

    constructor() {
        this.trackSidebarColumnWidth();
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

    /**
     * Publish the combined width of the columns beside the page (extension, preview) as
     * `--sideMenuRightInset`, so the offcanvas side menu fixed to the viewport's right edge stays
     * clear of them. Watched rather than read once: the columns open, close and resize by drag.
     */
    private trackSidebarColumnWidth(): void {
        effect((onCleanup) => {
            const columns: HTMLElement[] = [
                this.sidebarColumn()?.nativeElement,
                (this.previewColumn()?.nativeElement as HTMLElement)?.querySelector<HTMLElement>(
                    '.preview-resizable',
                ),
            ].filter(Boolean);
            // closed columns are `display: none` and count with a width of 0
            const update = () =>
                this.setSideMenuRightInset(
                    columns.reduce((sum, column) => sum + column.offsetWidth, 0),
                );
            const observer = new ResizeObserver(update);
            columns.forEach((column) => observer.observe(column));
            update();
            onCleanup(() => observer.disconnect());
        });
    }

    private setSideMenuRightInset(width: number): void {
        (this.host.nativeElement as HTMLElement).style.setProperty(
            '--sideMenuRightInset',
            `${width}px`,
        );
    }
}
