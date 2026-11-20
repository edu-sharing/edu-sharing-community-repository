import { LiveAnnouncer } from '@angular/cdk/a11y';
import {
    Component,
    EventEmitter,
    Input,
    NO_ERRORS_SCHEMA,
    Output,
    Pipe,
    PipeTransform,
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { NodeTitlePipe } from 'ngx-edu-sharing-ui';
import { SelectionBarComponent } from './selection-bar.component';

/** Renders translation keys with their `count` parameter, e.g. `SEARCH.N_SELECTED:2`. */
@Pipe({ name: 'translate', standalone: false })
class TranslatePipeStub implements PipeTransform {
    transform(key: string, params?: { count?: number }): string {
        return params?.count !== undefined ? `${key}:${params.count}` : key;
    }
}

/** Stand-in for the panel content: one checkbox per node that reports unchecking. */
@Component({
    selector: 'es-search-selection-overlay',
    template: `@for (node of nodes; track node.ref.id) {
        <input type="checkbox" checked (change)="deselect.emit(node)" />
        }`,
    standalone: false,
})
class SearchSelectionOverlayStubComponent {
    @Input() nodes: Node[];
    @Input() columns: unknown;
    @Input() displayType: unknown;
    @Output() deselect = new EventEmitter<Node>();
}

/** Page with a node list (its checkboxes), the bar with a projected action, and other content. */
@Component({
    template: `
        <input class="list-checkbox" type="checkbox" aria-label="SELECT:A" />
        <es-selection-bar
            class="visible"
            [nodes]="nodes"
            [(open)]="open"
            (clear)="cleared = true"
            (deselect)="remove($event)"
        >
            <button class="action" type="button">Action</button>
        </es-selection-bar>
        <button class="outside" type="button">Outside</button>
    `,
    standalone: false,
})
class HostComponent {
    nodes: Node[] = [];
    open = false;
    cleared = false;

    remove(node: Node): void {
        this.nodes = this.nodes.filter((n) => n !== node);
    }
}

describe('SelectionBarComponent', () => {
    let fixture: ComponentFixture<HostComponent>;
    let host: HostComponent;
    let bar: SelectionBarComponent;
    let announcer: jasmine.SpyObj<LiveAnnouncer>;

    const nodeA = { ref: { id: 'a' }, name: 'a.pdf', title: 'A' } as unknown as Node;
    const nodeB = { ref: { id: 'b' }, name: 'b.pdf', title: 'B' } as unknown as Node;

    const el = (selector: string) =>
        fixture.nativeElement.querySelector(selector) as HTMLElement | null;
    const toggle = () => el('.count') as HTMLButtonElement;
    const panel = () => el('.selection-panel');
    const panelCheckbox = () => el('.selection-panel input') as HTMLInputElement;

    beforeEach(() => {
        announcer = jasmine.createSpyObj<LiveAnnouncer>('LiveAnnouncer', ['announce']);
        announcer.announce.and.returnValue(Promise.resolve());
        TestBed.configureTestingModule({
            declarations: [
                HostComponent,
                SelectionBarComponent,
                SearchSelectionOverlayStubComponent,
                TranslatePipeStub,
            ],
            providers: [
                { provide: LiveAnnouncer, useValue: announcer },
                {
                    provide: TranslateService,
                    useValue: {
                        instant: (
                            key: string,
                            params?: { count?: number; name?: string; element?: string },
                        ) =>
                            params?.element !== undefined
                                ? `${key}:${params.element}`
                                : params?.name !== undefined
                                ? `${key}:${params.name}`
                                : params?.count !== undefined
                                ? `${key}:${params.count}`
                                : key,
                    },
                },
                { provide: NodeTitlePipe, useValue: { transform: (node: Node) => node.title } },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
        fixture = TestBed.createComponent(HostComponent);
        host = fixture.componentInstance;
        host.nodes = [nodeA, nodeB];
        document.body.appendChild(fixture.nativeElement);
        fixture.detectChanges();
        bar = fixture.debugElement.children
            .map((child) => child.componentInstance)
            .find((instance) => instance instanceof SelectionBarComponent);
    });

    afterEach(() => fixture.nativeElement.remove());

    function openPanel(): void {
        toggle().click();
        fixture.detectChanges();
        announcer.announce.calls.reset();
    }

    function pressEscape(target: HTMLElement): KeyboardEvent {
        const event = new KeyboardEvent('keydown', {
            key: 'Escape',
            bubbles: true,
            cancelable: true,
        });
        target.dispatchEvent(event);
        fixture.detectChanges();
        return event;
    }

    it('shows the count and keeps the panel closed initially', () => {
        expect(toggle().textContent).toContain('SEARCH.N_SELECTED:2');
        expect(toggle().getAttribute('aria-expanded')).toBe('false');
        expect(toggle().hasAttribute('aria-controls')).toBeFalse();
        expect(panel()).toBeNull();
    });

    it('opens the panel as a named dialog right after the toggle and before the actions', () => {
        toggle().click();
        fixture.detectChanges();

        expect(host.open).toBeTrue();
        expect(toggle().getAttribute('aria-expanded')).toBe('true');
        expect(panel().getAttribute('role')).toBe('dialog');
        expect(panel().getAttribute('aria-label')).toBe('SEARCH.SELECTION_PANEL');
        expect(toggle().getAttribute('aria-controls')).toBe(panel().id);
        expect(toggle().nextElementSibling).toBe(panel());
        expect(
            panel().compareDocumentPosition(el('.action')) &
                window.Node.DOCUMENT_POSITION_FOLLOWING,
        ).toBeTruthy();
        expect(announcer.announce).toHaveBeenCalledWith(
            'SEARCH.SELECTION_PANEL_SHOWN:SEARCH.SELECTION_PANEL',
        );
    });

    it('closes the panel on a second toggle and announces it', () => {
        openPanel();
        toggle().click();
        fixture.detectChanges();
        expect(host.open).toBeFalse();
        expect(panel()).toBeNull();
        expect(announcer.announce).toHaveBeenCalledWith(
            'SEARCH.SELECTION_PANEL_HIDDEN:SEARCH.SELECTION_PANEL',
        );
    });

    it('emits clear from the clear button', () => {
        (el('.clear-selection') as HTMLButtonElement).click();
        expect(host.cleared).toBeTrue();
    });

    describe('Escape', () => {
        beforeEach(() => openPanel());

        it('inside the panel closes it, focuses the toggle and stops propagation', () => {
            const pageListener = jasmine.createSpy('pageEscape');
            document.addEventListener('keydown', pageListener);
            panelCheckbox().focus();
            const event = pressEscape(panelCheckbox());
            document.removeEventListener('keydown', pageListener);

            expect(host.open).toBeFalse();
            expect(document.activeElement).toBe(toggle());
            expect(event.defaultPrevented).toBeTrue();
            expect(pageListener).not.toHaveBeenCalled();
        });

        it('on the toggle closes the panel', () => {
            toggle().focus();
            pressEscape(toggle());
            expect(host.open).toBeFalse();
            expect(document.activeElement).toBe(toggle());
        });

        it('on other controls of the bar is left to the page', () => {
            const action = el('.action');
            action.focus();
            const event = pressEscape(action);
            expect(host.open).toBeTrue();
            expect(event.defaultPrevented).toBeFalse();
            expect(document.activeElement).toBe(action);
        });
    });

    describe('document click', () => {
        beforeEach(() => openPanel());

        it('inside the panel keeps it open', () => {
            panelCheckbox().dispatchEvent(new MouseEvent('click', { bubbles: true }));
            fixture.detectChanges();
            expect(host.open).toBeTrue();
        });

        it('outside the bar closes it', () => {
            el('.outside').click();
            fixture.detectChanges();
            expect(host.open).toBeFalse();
            expect(announcer.announce).toHaveBeenCalledWith(
                'SEARCH.SELECTION_PANEL_HIDDEN:SEARCH.SELECTION_PANEL',
            );
        });
    });

    it('resets the open state without announcement once there are no nodes', () => {
        openPanel();
        host.nodes = [];
        fixture.detectChanges();
        fixture.detectChanges();
        expect(host.open).toBeFalse();
        expect(announcer.announce).not.toHaveBeenCalled();

        host.nodes = [nodeA];
        fixture.detectChanges();
        expect(panel()).toBeNull();
    });

    describe('unchecking a node in the panel', () => {
        beforeEach(() => openPanel());

        function uncheck(node: Node): void {
            bar.onDeselect(node);
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
        }

        it('moves focus to the toggle while nodes remain', fakeAsync(() => {
            panelCheckbox().focus();
            uncheck(nodeB);
            expect(host.nodes).toEqual([nodeA]);
            expect(document.activeElement).toBe(toggle());
            expect(toggle().textContent).toContain('SEARCH.N_SELECTED:1');
            expect(announcer.announce).not.toHaveBeenCalled();
        }));

        it('moves focus to the list checkbox of the last node', fakeAsync(() => {
            uncheck(nodeB);
            panelCheckbox().focus();
            uncheck(nodeA);
            expect(document.activeElement).toBe(el('.list-checkbox'));
        }));

        it('announces the new count without focus in the panel', fakeAsync(() => {
            el('.outside').focus();
            uncheck(nodeB);
            expect(document.activeElement).toBe(el('.outside'));
            expect(announcer.announce).toHaveBeenCalledWith('SEARCH.N_SELECTED:1');
        }));
    });
});
