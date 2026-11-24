import { Component, inject, Input } from '@angular/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { FacetsDict } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Observable } from 'rxjs';
import { SharedModule } from '../../../../../shared/shared.module';
import { MdsWidget } from '../../../types/types';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetSearchSuggestionsComponent } from './mds-editor-widget-search-suggestions.component';

/**
 * Widget dummy that keeps its values locally, since the search suggestions widget reads and writes
 * them through the editor instance the plain `WidgetDummy` does not have.
 */
class SearchSuggestionsWidgetDummy extends WidgetDummy {
    private readonly valuesSubject = new BehaviorSubject<string[]>([]);

    _new_getValue(): string[] {
        return this.valuesSubject.value;
    }

    _new_observeValue(): Observable<string[]> {
        return this.valuesSubject.asObservable();
    }

    _new_setValue(value: string[]): void {
        this.valuesSubject.next(value);
    }
}

/**
 * Renders the widget like inside the suggestions panel of the search field: in suggestion mode,
 * fed with the given suggestions, on the panel background.
 */
@Component({
    selector: 'es-search-suggestions-story-host',
    template: `
        <div class="suggestion-card">
            <es-mds-editor-widget-search-suggestions
                [widget]="widget"
            ></es-mds-editor-widget-search-suggestions>
            <p class="added">Added filters: {{ (addedValues$ | async)?.join(', ') || '–' }}</p>
        </div>
    `,
    styles: [
        `
            .suggestion-card {
                max-width: 400px;
                padding: 10px;
                background-color: var(--backgroundColor);
                color: var(--textMain);
            }
            .added {
                margin-top: 16px;
                font-size: 12px;
                color: var(--textLight);
            }
        `,
    ],
    standalone: false,
})
class SearchSuggestionsStoryHostComponent {
    private readonly mdsEditorInstance = inject(MdsEditorInstanceService);

    widget: Widget;
    addedValues$: Observable<string[]>;

    @Input() set args(args: SearchSuggestionsStoryArgs) {
        // Storybook may pass the same args repeatedly; the widget component subscribes to the
        // values of the instance it received first, so keep it unless its definition changes.
        const definition = this.widget?.definition;
        if (definition?.id !== args.property || definition?.caption !== args.caption) {
            const widget = new SearchSuggestionsWidgetDummy({
                id: args.property,
                caption: args.caption,
                type: 'multivalueSuggestBadges',
            } as MdsWidget);
            this.widget = widget as unknown as Widget;
            this.addedValues$ = widget._new_observeValue();
        }
        this.mdsEditorInstance.editorMode = 'searchFacetSuggestion';
        this.mdsEditorInstance.suggestionsSubject.next(toFacets(args.property, args.suggestions));
    }
}

interface SearchSuggestionsStoryArgs {
    /** Property the suggestions belong to; selects the chip colour. */
    property: string;
    /** Category label, shown as "Filter by: <caption>". */
    caption: string;
    /** Suggested values; an activated suggestion becomes a filter and leaves the list. */
    suggestions: string[];
}

function toFacets(property: string, suggestions: string[]): FacetsDict {
    return {
        [property]: {
            hasMore: false,
            values: suggestions.map((value) => ({ value, label: value, count: 1 })),
        },
    } as FacetsDict;
}

const meta: Meta<SearchSuggestionsStoryArgs> = {
    title: 'Mds/Widget/Search Suggestions',
    component: SearchSuggestionsStoryHostComponent,
    decorators: [
        moduleMetadata({
            declarations: [
                MdsEditorWidgetSearchSuggestionsComponent,
                MdsEditorWidgetContainerComponent,
                RegisterFormFieldDirective,
            ],
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    argTypes: {
        property: {
            control: 'select',
            options: [
                'cclom:general_keyword',
                'ccm:educationalcontext',
                'ccm:taxonid',
                'ccm:objecttype',
                'ccm:unknown_property',
            ],
        },
        caption: { control: 'text' },
        suggestions: { control: 'object' },
    },
    args: {
        property: 'cclom:general_keyword',
        caption: 'Schlagworte',
        suggestions: ['Demo', 'Demokratie', 'Demografie'],
    },
    tags: ['autodocs'],
    render: (args) => ({
        props: { args },
        template: `<es-search-suggestions-story-host [args]="args"></es-search-suggestions-story-host>`,
    }),
};

export default meta;
type Story = StoryObj<SearchSuggestionsStoryArgs>;

/** Several suggestions of one category; each one is a button that adds it as filter. */
export const Default: Story = {};

/** A single suggestion. */
export const SingleSuggestion: Story = {
    args: {
        suggestions: ['Demo'],
    },
};

/** Long labels wrap inside the chip instead of overflowing the panel. */
export const LongLabels: Story = {
    args: {
        property: 'ccm:taxonid',
        caption: 'Fach',
        suggestions: [
            'Wirtschaft, Arbeit und Technik mit einem besonders langen Namen',
            'Mathematik',
        ],
    },
};

/** A property without its own chip colour falls back to the default chip background. */
export const UnknownProperty: Story = {
    args: {
        property: 'ccm:unknown_property',
        caption: 'Sonstiges',
        suggestions: ['Wert A', 'Wert B'],
    },
};
