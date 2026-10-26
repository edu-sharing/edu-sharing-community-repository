import {
    afterNextRender,
    Component,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    input,
    viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Node, ParentEntries } from 'ngx-edu-sharing-api';
import { TemplateComponent } from '../../../features/topic-page/editor/template.component';
import { CollectionViewSlotsService } from '../collection-view-slots.service';

/**
 * The topic page of a collection, shown as a view of the collections page below its breadcrumb.
 * Loaded on demand, so the collections page does not carry the topic page code.
 */
@Component({
    selector: 'es-collection-topic-view',
    imports: [TemplateComponent],
    templateUrl: './collection-topic-view.component.html',
    styleUrls: ['./collection-topic-view.component.scss'],
})
export class CollectionTopicViewComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly slots = inject(CollectionViewSlotsService);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly destroyRef = inject(DestroyRef);

    readonly collection = input.required<Node>();
    /** ancestors of the collection as the collections page loaded them, the collection first */
    readonly parents = input<ParentEntries | null>(null);
    /** the variant the URL selects when the view opens; later selections are kept by the page */
    readonly initialVariantId: string | undefined =
        this.route.snapshot.queryParamMap.get('variantId') ?? undefined;
    private readonly template = viewChild.required(TemplateComponent);

    constructor() {
        effect(() => this.slots.breadcrumbEnd.set(this.template().variantSelectTemplate()));
        this.destroyRef.onDestroy(() => this.slots.breadcrumbEnd.set(null));
        afterNextRender(() => this.trackSideMenuInset());
    }

    /**
     * The topic page's side menu is fixed to the viewport, beside the right edge of the page's card,
     * which moves with the open sidebar and its width.
     */
    private trackSideMenuInset(): void {
        const host: HTMLElement = this.host.nativeElement;
        const card: HTMLElement | null = host.closest('.main-content');
        if (!card) {
            return;
        }
        const updateInset = () => {
            const margin = parseFloat(
                getComputedStyle(host).getPropertyValue('--roundedContainerMargin'),
            );
            const inset = window.innerWidth - card.getBoundingClientRect().right - (margin || 0);
            host.style.setProperty('--sideMenuRightInset', `${Math.max(0, inset)}px`);
        };
        const observer = new ResizeObserver(updateInset);
        observer.observe(card);
        window.addEventListener('resize', updateInset);
        updateInset();
        this.destroyRef.onDestroy(() => {
            observer.disconnect();
            window.removeEventListener('resize', updateInset);
        });
    }

    /** Opens the collection of a topic linked inside the page, which then chooses its view. */
    readonly openCollection = (collectionId: string): void => {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { id: collectionId },
            queryParamsHandling: 'merge',
        });
    };
}
