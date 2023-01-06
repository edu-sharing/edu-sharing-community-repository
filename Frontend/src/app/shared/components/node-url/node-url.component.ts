import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild,
} from '@angular/core';
import { Node } from '../../../core-module/rest/data-object';
import { ListTableComponent } from '../../../core-ui-module/components/list-table/list-table.component';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';

// TODO: Decide if providing focus highlights and ripples with this component is a good idea. When
// using `app-node-url` for cards, we might need highlights and ripples for the whole card while
// `app-node-url` should only wrap the title since links with lots of content confuse screen
// readers.

const NODE_URL_TAG_NAME = 'es-node-url';

@Component({
    selector: NODE_URL_TAG_NAME,
    templateUrl: 'node-url.component.html',
    styleUrls: ['node-url.component.scss'],
})
export class NodeUrlComponent implements AfterViewInit {
    @ViewChild('link') link: ElementRef<HTMLAnchorElement>;

    @Input() listTable: ListTableComponent;
    @Input() node: Node;
    @Input() nodes: Node[];
    @Input() target: string;
    @Input() scope: string;
    /**
     * custom query params to include
     */
    @Input() queryParams: { [key: string]: string | number | boolean } = {};
    /**
     * link: a element
     * button: button element
     * wrapper: div element with behavior "like" a link
     */
    @Input() mode: 'link' | 'button' | 'wrapper' = 'link';
    @Input() disabled = false;
    /**
     * Show the ripple effect even when disabled.
     *
     * @deprecated Temporary workaround for list-table, which *sometimes* uses it's on click
     * bindings.
     */
    @Input() alwaysRipple = false;
    @Input('aria-describedby') ariaDescribedby: string;
    @Input('aria-label') ariaLabel = true;

    @Output() buttonClick = new EventEmitter<MouseEvent>();

    /**
     * Whether this instance of `NodeUrl` is nested inside another `NodeUrl`.
     */
    // We use nested `NodeUrl`s for a11y where we have a `NodeUrl` in wrapper mode to maximize
    // clickable area and one in  link mode that only contains the title. We only want the outmost
    // `NodeUrl` to apply the ripple effect.
    //
    // Note that nesting `NodeUrl`s is only necessary when we want to provide hover effects on parts
    // of the outer `NodeUrl`. If we don't need that, it would be easier to attach a pseudo `:after`
    // element to the inner `NodeUrl` that expands its click area.
    isNested: boolean;

    constructor(
        private nodeHelper: NodeHelperService,
        private elementRef: ElementRef<HTMLElement>,
    ) {}

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.isNested = this.getIsNested();
        });
    }

    getState() {
        return {
            scope: this.scope,
        };
    }

    get(mode: 'routerLink' | 'queryParams'): any {
        const result: any = this.nodeHelper.getNodeLink(mode, this.node);
        if (mode === 'queryParams' && this.queryParams) {
            Object.keys(this.queryParams).forEach((k) => (result[k] = this.queryParams[k]));
        }
        return result;
    }

    focus(): void {
        this.link.nativeElement.focus();
    }

    clickWrapper(event: MouseEvent) {
        const eventCopy = copyClickEvent(event);
        this.link.nativeElement.dispatchEvent(eventCopy);
        event.preventDefault();
    }

    private getIsNested(): boolean {
        let ancestor = this.elementRef.nativeElement.parentElement;
        while (ancestor) {
            if (ancestor.tagName === NODE_URL_TAG_NAME.toUpperCase()) {
                return true;
            }
            ancestor = ancestor.parentElement;
        }
        return false;
    }
}

function copyClickEvent(event: MouseEvent): MouseEvent {
    // On Firefox, a middle click via neither the 'click', nor the 'auxclick' event cause a new tab
    // to be opened when triggered programmatically. As a workaround, we simulate a ctrl click
    // instead of a middle click. This matches Firefox's defaults, but we cannot account for
    // changed middle-click behavior.
    if (event.type === 'auxclick' && event.button === 1 && isFirefox()) {
        return copyClickEvent({ ...event, type: 'click', ctrlKey: true, button: 0 });
    }
    // It would seem better to use `event.type` instead of hard-coding 'click', but that doesn't
    // have the desired effect for non-click events when dispatched.
    return new MouseEvent('click', {
        cancelable: true,
        button: event.button,
        ctrlKey: event.ctrlKey,
        shiftKey: event.shiftKey,
        altKey: event.altKey,
        metaKey: event.metaKey,
    });
}

function isFirefox(): boolean {
    return navigator.userAgent.includes('Firefox');
}
