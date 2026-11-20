import { Component, inject, Injectable, Input } from '@angular/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import {
    DEFAULT,
    FacetsDict,
    HOME_REPOSITORY,
    LabeledValue,
    LabeledValuesDict,
    RawValuesDict,
} from 'ngx-edu-sharing-api';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { mdsStorybookProviders } from '../../../features/mds/mds-editor/storybook-utils';
import { MdsModule } from '../../../features/mds/mds.module';
import { SharedModule } from '../../../shared/shared.module';
import { LoadingScreenService } from '../../loading-screen/loading-screen.service';
import { SearchFieldInternalService } from './search-field-internal.service';
import { SearchFieldComponent } from './search-field.component';
import { SearchFieldConfig } from './search-field.service';

/** Values the fake backend suggests per property, matched against the typed text. */
const SUGGESTION_VALUES: { [property: string]: LabeledValue[] } = {
    'cclom:general_keyword': [
        'Demo',
        'Demokratie',
        'Demografie',
        'Dokumentation',
        'Mathematik',
        'Musik',
    ].map((value) => ({ value, label: value })),
    'ccm:educationallearningresourcetype': [
        { value: 'demonstration', label: 'Demonstration' },
        { value: 'document', label: 'Dokument' },
        { value: 'video', label: 'Video' },
    ],
};

/**
 * Stand-in for `SearchFieldInternalService` that keeps filters locally and answers suggestion
 * requests from `SUGGESTION_VALUES`, so the search field works without a search page or backend.
 */
@Injectable()
class SearchFieldInternalServiceStoryMock {
    readonly config = new BehaviorSubject<SearchFieldConfig>({
        placeholder: 'SEARCH.SEARCH_STUFF',
        showFiltersButton: true,
        enableFiltersAndSuggestions: true,
    } as SearchFieldConfig);
    readonly searchFieldComponent = new BehaviorSubject<SearchFieldComponent>(null);
    readonly filtersButtonClicked = new Subject<void>();
    readonly filterBarVisible = new BehaviorSubject(false);
    readonly filterPanelId = new BehaviorSubject<string>(null);
    readonly searchStringChanged = new Subject<string>();
    readonly searchString = new BehaviorSubject('');
    readonly categoriesSubject = new BehaviorSubject<string[]>(null);
    readonly mdsInfo$ = of({ repository: HOME_REPOSITORY, metadataSet: DEFAULT });
    readonly filters$ = new BehaviorSubject<LabeledValuesDict>({});
    readonly rawFilters$ = this.filters$.pipe(
        map((filters) =>
            Object.fromEntries(
                Object.entries(filters).map(([property, values]) => [
                    property,
                    values.map((value) => value.value),
                ]),
            ),
        ),
    );
    readonly suggestions$ = new BehaviorSubject<FacetsDict>(null);
    /** The last search triggered by the field, shown below it in the story. */
    readonly lastSearch = new BehaviorSubject<string>(null);

    updateSuggestions(inputString: string): void {
        const search = inputString?.trim().toLowerCase();
        if (!search || search.length < 2) {
            this.suggestions$.next(null);
            return;
        }
        const suggestions: FacetsDict = {};
        for (const property of this.categoriesSubject.value ?? []) {
            const values = (SUGGESTION_VALUES[property] ?? []).filter((value) =>
                value.label.toLowerCase().includes(search),
            );
            suggestions[property] = {
                hasMore: false,
                values: values.map((value) => ({ ...value, count: 1 })),
            } as FacetsDict[string];
        }
        this.suggestions$.next(suggestions);
    }

    setFilterValues(values: RawValuesDict): void {
        const filters: LabeledValuesDict = {};
        for (const [property, rawValues] of Object.entries(values ?? {})) {
            if (rawValues?.length) {
                filters[property] = rawValues.map(
                    (value) =>
                        SUGGESTION_VALUES[property]?.find((known) => known.value === value) ?? {
                            value,
                            label: value,
                        },
                );
            }
        }
        this.filters$.next(filters);
    }

    removeFilter(property: string, filter: LabeledValue): void {
        const filters = { ...this.filters$.value };
        filters[property] = filters[property]?.filter((value) => value.value !== filter.value);
        if (!filters[property]?.length) {
            delete filters[property];
        }
        this.filters$.next(filters);
    }

    triggerSearch({ searchString }: { searchString: string }): void {
        this.lastSearch.next(searchString ?? '');
    }
}

interface SearchFieldStoryArgs {
    /** Active filters per property at start, e.g. `{ "cclom:general_keyword": ["Doku"] }`. */
    activeFilters: RawValuesDict;
    /** Shows the filter button at the start of the field. */
    showFiltersButton: boolean;
}

/**
 * Places the search field in a dark bar like the page header, followed by page content, so the
 * suggestions panel overlays something and Tab can leave the field.
 */
@Component({
    selector: 'es-search-field-story-host',
    template: `
        <div class="top-bar">
            <es-search-field></es-search-field>
        </div>
        <main class="content">
            <p>
                Type at least two letters, e.g. „Dem“ or „Mu“. Tab or arrow down moves into the
                panel, Esc closes it.
            </p>
            <button type="button">Next element on the page</button>
            <p class="last-search">Last search: {{ (lastSearch | async) ?? '–' }}</p>
        </main>
    `,
    styles: [
        `
            .top-bar {
                display: flex;
                align-items: center;
                padding: 8px 16px;
                background-color: var(--workspaceTopBarBackground);
            }
            es-search-field {
                width: 100%;
                max-width: 600px;
            }
            .content {
                padding: 16px;
                min-height: 300px;
                background-color: var(--backgroundColor);
                color: var(--textMain);
            }
            .last-search {
                font-size: 12px;
                color: var(--textLight);
            }
        `,
    ],
    standalone: false,
})
class SearchFieldStoryHostComponent {
    private readonly internal = inject(
        SearchFieldInternalService,
    ) as unknown as SearchFieldInternalServiceStoryMock;
    readonly lastSearch = this.internal.lastSearch;

    @Input() set args(args: SearchFieldStoryArgs) {
        this.internal.config.next({
            ...this.internal.config.value,
            showFiltersButton: args.showFiltersButton,
        });
        this.internal.setFilterValues(args.activeFilters);
    }
}

const meta: Meta<SearchFieldStoryArgs> = {
    title: 'Navigation/Search Field',
    component: SearchFieldStoryHostComponent,
    decorators: [
        moduleMetadata({
            declarations: [SearchFieldComponent],
            imports: [SharedModule, MdsModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                {
                    provide: SearchFieldInternalService,
                    useClass: SearchFieldInternalServiceStoryMock,
                },
                { provide: LoadingScreenService, useValue: { observeIsLoading: () => of(false) } },
            ],
        }),
    ],
    argTypes: {
        activeFilters: { control: 'object' },
        showFiltersButton: { control: 'boolean' },
    },
    args: {
        activeFilters: {},
        showFiltersButton: true,
    },
    tags: ['autodocs'],
    render: (args) => ({
        props: { args },
        template: `<es-search-field-story-host [args]="args"></es-search-field-story-host>`,
    }),
};

export default meta;
type Story = StoryObj<SearchFieldStoryArgs>;

/** Empty search field; suggestions appear while typing. */
export const Default: Story = {};

/** With active filters, which the panel lists above the suggestions and lets remove. */
export const WithActiveFilters: Story = {
    args: {
        activeFilters: {
            'cclom:general_keyword': ['Dokumentation'],
            'ccm:educationallearningresourcetype': ['video'],
        },
    },
};
