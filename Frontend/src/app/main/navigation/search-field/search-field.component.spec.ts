import { LiveAnnouncer } from '@angular/cdk/a11y';
import { ElementRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { FacetsDict, LabeledValuesDict, MdsService } from 'ngx-edu-sharing-api';
import { PropertySlugPipe } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, NEVER, Subject } from 'rxjs';
import { LoadingScreenService } from '../../loading-screen/loading-screen.service';
import { SearchFieldInternalService } from './search-field-internal.service';
import { SearchFieldComponent } from './search-field.component';

/** Minimal stand-in for `SearchFieldInternalService` with controllable streams. */
class SearchFieldInternalServiceStub {
    readonly config = new BehaviorSubject({ placeholder: 'SEARCH.SEARCH_STUFF' });
    readonly searchFieldComponent = new BehaviorSubject<SearchFieldComponent>(null);
    readonly filtersButtonClicked = new Subject<void>();
    readonly filterBarVisible = new BehaviorSubject(false);
    readonly filterPanelId = new BehaviorSubject<string>(null);
    readonly searchStringChanged = new Subject<string>();
    readonly searchString = new BehaviorSubject('');
    readonly categoriesSubject = new BehaviorSubject<string[]>(['cclom:general_keyword']);
    readonly filters$ = new BehaviorSubject<LabeledValuesDict>({});
    readonly rawFilters$ = new BehaviorSubject<{ [property: string]: string[] }>({});
    readonly suggestions$ = new BehaviorSubject<FacetsDict>(null);
    readonly mdsInfo$ = NEVER;
    updateSuggestions = jasmine.createSpy('updateSuggestions');
    triggerSearch = jasmine.createSpy('triggerSearch');
    setFilterValues = jasmine.createSpy('setFilterValues');
    removeFilter = jasmine.createSpy('removeFilter');
}

const loadingScreenStub = { observeIsLoading: () => new BehaviorSubject(false) };
const translateStub = {
    instant: (key: string, params?: object) => (params ? `${key} ${JSON.stringify(params)}` : key),
};

function facets(values: { [property: string]: string[] }): FacetsDict {
    const result: FacetsDict = {};
    for (const [property, list] of Object.entries(values)) {
        result[property] = {
            hasMore: false,
            values: list.map((value) => ({ value, label: value, count: 1 })),
        } as FacetsDict[string];
    }
    return result;
}

describe('SearchFieldComponent (logic)', () => {
    let component: SearchFieldComponent;
    let internal: SearchFieldInternalServiceStub;
    let announcer: jasmine.SpyObj<LiveAnnouncer>;
    let host: HTMLElement;
    let input: HTMLInputElement;
    /** A button of the field outside the panel, like the filters or clear button. */
    let fieldButton: HTMLButtonElement;
    let panel: HTMLElement;
    let panelButton: HTMLButtonElement;

    beforeEach(() => {
        internal = new SearchFieldInternalServiceStub();
        announcer = jasmine.createSpyObj<LiveAnnouncer>('LiveAnnouncer', ['announce']);
        host = document.createElement('div');
        input = document.createElement('input');
        fieldButton = document.createElement('button');
        panel = document.createElement('div');
        panelButton = document.createElement('button');
        panel.append(panelButton);
        host.append(input, fieldButton, panel);
        document.body.appendChild(host);
        TestBed.configureTestingModule({
            providers: [
                SearchFieldComponent,
                { provide: ElementRef, useValue: new ElementRef(host) },
                { provide: SearchFieldInternalService, useValue: internal },
                { provide: LoadingScreenService, useValue: loadingScreenStub },
                { provide: MdsService, useValue: {} },
                { provide: LiveAnnouncer, useValue: announcer },
                { provide: TranslateService, useValue: translateStub },
            ],
        });
        component = TestBed.inject(SearchFieldComponent);
        component.input = new ElementRef(input);
        (component as any).panel = new ElementRef(panel);
        component.ngOnInit();
    });

    afterEach(() => {
        component.ngOnDestroy();
        host.remove();
    });

    describe('panelOpen', () => {
        it('is closed while the panel has not been requested', () => {
            component.filtersCount = 2;
            expect(component.panelOpen).toBeFalse();
        });

        it('opens when requested and there are active filters', () => {
            internal.suggestions$.next(null);
            component.filtersCount = 1;
            component.showOverlay.next(true);
            expect(component.panelOpen).toBeTrue();
        });

        it('opens when requested and there are suggestions', () => {
            component.filtersCount = 0;
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            component.showOverlay.next(true);
            expect(component.panelOpen).toBeTrue();
        });

        it('stays closed when there is nothing to show', () => {
            component.filtersCount = 0;
            internal.suggestions$.next(facets({ 'cclom:general_keyword': [] }));
            component.showOverlay.next(true);
            expect(component.panelOpen).toBeFalse();
        });

        it('stays closed while the page is loading', () => {
            component.filtersCount = 1;
            component.showOverlay.next(true);
            component.isLoading = true;
            expect(component.panelOpen).toBeFalse();
        });
    });

    describe('closing', () => {
        beforeEach(() => {
            component.filtersCount = 1;
            component.showOverlay.next(true);
        });

        it('keeps the panel open on clicks inside the search field', () => {
            const event = new MouseEvent('click');
            spyOn(event, 'composedPath').and.returnValue([panelButton, host, document.body]);
            component.onDocumentClick(event);
            expect(component.panelOpen).toBeTrue();
        });

        it('keeps the panel open on clicks on elements its own click handler removed', () => {
            // the path is fixed at dispatch time and still contains the host
            const detached = document.createElement('button');
            const event = new MouseEvent('click');
            spyOn(event, 'composedPath').and.returnValue([detached, host]);
            component.onDocumentClick(event);
            expect(component.panelOpen).toBeTrue();
        });

        it('closes the panel on clicks elsewhere on the page', () => {
            const event = new MouseEvent('click');
            spyOn(event, 'composedPath').and.returnValue([document.body]);
            component.onDocumentClick(event);
            expect(component.panelOpen).toBeFalse();
        });

        it('keeps the panel open while the focus moves within the search field', () => {
            component.onFocusOut(new FocusEvent('focusout', { relatedTarget: panelButton }));
            expect(component.panelOpen).toBeTrue();
        });

        it('closes the panel when the focus leaves the search field', () => {
            const outside = document.createElement('button');
            component.onFocusOut(new FocusEvent('focusout', { relatedTarget: outside }));
            expect(component.panelOpen).toBeFalse();
        });

        it('closes the panel when the focus moves to nowhere', () => {
            component.onFocusOut(new FocusEvent('focusout', { relatedTarget: null }));
            expect(component.panelOpen).toBeFalse();
        });

        describe('Escape', () => {
            let reachedPage: boolean;
            const onHostKeydown = (event: KeyboardEvent) => {
                if (event.key === 'Escape') {
                    component.onEscape(event);
                }
            };
            const onPageKeydown = () => (reachedPage = true);

            beforeEach(() => {
                reachedPage = false;
                // the host listener of the component and a page-level Esc handler (e.g. a sidebar)
                host.addEventListener('keydown', onHostKeydown);
                document.addEventListener('keydown', onPageKeydown);
            });

            afterEach(() => {
                host.removeEventListener('keydown', onHostKeydown);
                document.removeEventListener('keydown', onPageKeydown);
            });

            function pressEscape(element: HTMLElement): KeyboardEvent {
                element.focus();
                const event = new KeyboardEvent('keydown', {
                    key: 'Escape',
                    bubbles: true,
                    cancelable: true,
                });
                element.dispatchEvent(event);
                return event;
            }

            it('closes the panel from inside it and returns the focus to the input', () => {
                const event = pressEscape(panelButton);
                expect(component.panelOpen).toBeFalse();
                expect(document.activeElement).toBe(input);
                expect(event.defaultPrevented).toBeTrue();
                expect(reachedPage).toBeFalse();
            });

            it('closes the panel from the input and keeps the focus there', () => {
                const event = pressEscape(input);
                expect(component.panelOpen).toBeFalse();
                expect(document.activeElement).toBe(input);
                // also keeps the browser from clearing the search input
                expect(event.defaultPrevented).toBeTrue();
                expect(reachedPage).toBeFalse();
            });

            it('ignores Escape on other buttons of the field', () => {
                const event = pressEscape(fieldButton);
                expect(component.panelOpen).toBeTrue();
                expect(document.activeElement).toBe(fieldButton);
                expect(event.defaultPrevented).toBeFalse();
                expect(reachedPage).toBeTrue();
            });

            it('ignores Escape while the panel is closed', () => {
                component.showOverlay.next(false);
                const event = pressEscape(input);
                // the browser keeps its default behavior (clearing the search input)
                expect(event.defaultPrevented).toBeFalse();
                expect(reachedPage).toBeTrue();
            });
        });
    });

    describe('focusPanel', () => {
        let facetsFocus: jasmine.Spy;

        beforeEach(() => {
            facetsFocus = jasmine.createSpy('focus');
            (component as any).searchFieldFacets = { focus: facetsFocus };
        });

        it('focuses the first active filter if there is one', () => {
            const chipFocus = jasmine.createSpy('chipFocus');
            (component as any).firstActiveChip = { focus: chipFocus };
            component.filtersCount = 1;
            component.showOverlay.next(true);
            component.focusPanel(new KeyboardEvent('keydown'));
            expect(chipFocus).toHaveBeenCalled();
            expect(facetsFocus).not.toHaveBeenCalled();
        });

        it('focuses the first suggestion without active filters', () => {
            component.filtersCount = 0;
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            component.showOverlay.next(true);
            component.focusPanel(new KeyboardEvent('keydown'));
            expect(facetsFocus).toHaveBeenCalled();
        });

        it('opens a closed panel first and focuses it once rendered', fakeAsync(() => {
            component.filtersCount = 0;
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            component.showOverlay.next(false);
            const event = new KeyboardEvent('keydown', { cancelable: true });
            component.focusPanel(event);
            expect(component.panelOpen).toBeTrue();
            expect(event.defaultPrevented).toBeTrue();
            expect(facetsFocus).not.toHaveBeenCalled();
            tick();
            expect(facetsFocus).toHaveBeenCalled();
        }));
    });

    describe('suggestion announcement', () => {
        /**
         * Re-subscribes the component inside the current fakeAsync zone and simulates typing. RxJS reuses the interval
         * of a pending `debounceTime`, so a subscription made outside the fake zone would keep
         * using real timers that `tick()` cannot advance.
         */
        function restartInFakeZone(): void {
            component.ngOnDestroy();
            (component as any).destroyed$ = new Subject<void>();
            component.ngOnInit();
            // the user is typing into the focused search field
            component.inputHasFocus = true;
            component.inputControl.setValue('Dem', { emitEvent: false });
        }

        it('announces the number of suggestions', fakeAsync(() => {
            restartInFakeZone();
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo', 'Demokratie'] }));
            tick(500);
            expect(announcer.announce).toHaveBeenCalledWith('SEARCH.SUGGESTIONS_COUNT {"count":2}');
        }));

        it('uses the singular for a single suggestion', fakeAsync(() => {
            restartInFakeZone();
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            tick(500);
            expect(announcer.announce).toHaveBeenCalledWith(
                'SEARCH.SUGGESTIONS_COUNT_ONE {"count":1}',
            );
        }));

        it('does not count values that are already set as filter', fakeAsync(() => {
            restartInFakeZone();
            internal.rawFilters$.next({ 'cclom:general_keyword': ['Demo'] });
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo', 'Demokratie'] }));
            tick(500);
            expect(announcer.announce).toHaveBeenCalledWith(
                'SEARCH.SUGGESTIONS_COUNT_ONE {"count":1}',
            );
        }));

        it('announces once the suggestions settle while typing', fakeAsync(() => {
            restartInFakeZone();
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['A', 'B', 'C'] }));
            tick(200);
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['A'] }));
            tick(500);
            expect(announcer.announce).toHaveBeenCalledTimes(1);
            expect(announcer.announce).toHaveBeenCalledWith(
                'SEARCH.SUGGESTIONS_COUNT_ONE {"count":1}',
            );
        }));

        it('drops a pending announcement when the component is destroyed', fakeAsync(() => {
            restartInFakeZone();
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            component.ngOnDestroy();
            tick(500);
            expect(announcer.announce).not.toHaveBeenCalled();
        }));

        it('stays silent while the input is not focused', fakeAsync(() => {
            restartInFakeZone();
            component.inputHasFocus = false;
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            tick(500);
            expect(announcer.announce).not.toHaveBeenCalled();
        }));

        it('stays silent while the input is empty', fakeAsync(() => {
            restartInFakeZone();
            component.inputControl.setValue('', { emitEvent: false });
            internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
            tick(500);
            expect(announcer.announce).not.toHaveBeenCalled();
        }));
    });

    it('returns the focus to the input after a suggestion was added', () => {
        panelButton.focus();
        component.onValuesChange({ 'cclom:general_keyword': ['Demo'] });
        expect(internal.setFilterValues).toHaveBeenCalled();
        expect(component.inputControl.value).toBe('');
        expect(document.activeElement).toBe(input);
    });
});

describe('SearchFieldComponent (template)', () => {
    let fixture: ComponentFixture<SearchFieldComponent>;
    let internal: SearchFieldInternalServiceStub;

    beforeEach(() => {
        internal = new SearchFieldInternalServiceStub();
        TestBed.configureTestingModule({
            declarations: [SearchFieldComponent, PropertySlugPipe],
            imports: [ReactiveFormsModule, TranslateModule.forRoot()],
            providers: [
                { provide: SearchFieldInternalService, useValue: internal },
                { provide: LoadingScreenService, useValue: loadingScreenStub },
                { provide: MdsService, useValue: {} },
            ],
            // child components (facets, chips, icons) are not under test here
            schemas: [NO_ERRORS_SCHEMA],
        });
        fixture = TestBed.createComponent(SearchFieldComponent);
        fixture.detectChanges();
    });

    function query<T extends Element>(selector: string): T {
        return fixture.nativeElement.querySelector(selector);
    }

    it('exposes the input as combobox that controls the panel', () => {
        const input = query<HTMLInputElement>('input.search-input');
        const panel = query<HTMLElement>('.suggestion-card');
        expect(input.getAttribute('role')).toBe('combobox');
        expect(input.getAttribute('aria-haspopup')).toBe('dialog');
        expect(input.getAttribute('aria-controls')).toBe(panel.id);
        expect(panel.getAttribute('role')).toBe('dialog');
        expect(panel.getAttribute('aria-label')).toBe('SEARCH.SUGGESTIONS_PANEL');
    });

    it('renders the panel directly after the search form', () => {
        const panel = query<HTMLElement>('.suggestion-card');
        expect(panel.previousElementSibling.tagName).toBe('FORM');
        expect(panel.closest('.cdk-overlay-container')).toBeNull();
    });

    it('reflects the panel state in aria-expanded and hidden', () => {
        const input = query<HTMLInputElement>('input.search-input');
        const panel = query<HTMLElement>('.suggestion-card');
        expect(input.getAttribute('aria-expanded')).toBe('false');
        expect(panel.hidden).toBeTrue();

        internal.suggestions$.next(facets({ 'cclom:general_keyword': ['Demo'] }));
        fixture.componentInstance.showOverlay.next(true);
        fixture.detectChanges();
        expect(input.getAttribute('aria-expanded')).toBe('true');
        expect(panel.hidden).toBeFalse();
    });

    it('uses a unique panel id per instance', () => {
        const other = TestBed.createComponent(SearchFieldComponent);
        other.detectChanges();
        expect(other.componentInstance.panelId).not.toBe(fixture.componentInstance.panelId);
    });
});
