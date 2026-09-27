import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { MdsEditorWrapperComponent } from './mds-editor-wrapper.component';
import {
    Data,
    DefaultMds,
    DummyNode,
    mdsStorybookProviders,
    registerMockNode,
} from '../storybook-utils';
import { SharedModule } from '../../../../shared/shared.module';
import { DEFAULT, MdsDefinition, MdsIdentifier, MdsService, Node } from 'ngx-edu-sharing-api';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../mds.module';
import { Helper, OptionItem, OptionsHelperDataService, Target } from 'ngx-edu-sharing-ui';
import { Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { expect, userEvent, waitFor, within } from '@storybook/test';

/** the `mat-form-field` a widget is rendered in, see `MdsEditorWidgetContainerComponent` */
function getFormField(canvasElement: HTMLElement, widgetId: string): HTMLElement {
    const field = canvasElement.querySelector<HTMLElement>(
        `.widget-form-field-${widgetId.replace(':', '-')}`,
    );
    expect(field, `form field of ${widgetId}`).toBeTruthy();
    return field;
}

/** the whole widget block, i.e. the form field plus the header with the bulk "edit" toggle */
function getWidgetContainer(canvasElement: HTMLElement, widgetId: string): HTMLElement {
    return getFormField(canvasElement, widgetId).closest<HTMLElement>('.widget-container');
}

const meta: Meta<MdsEditorWrapperComponent> = {
    title: 'Mds/Editor',
    component: MdsEditorWrapperComponent,
    decorators: [
        moduleMetadata({
            declarations: [],
            imports: [MdsModule, SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    render: (args) => ({
        props: {
            ...args,
            save: async (mds: MdsEditorWrapperComponent) =>
                alert(JSON.stringify(await mds.onSave(), null, 2)), // <-- your callback
        },
        template: `
      <es-mds-editor-wrapper #mds [embedded]="embedded" [setId]="setId" [groupId]="groupId" [editorMode]="editorMode" [nodes]="nodes"></es-mds-editor-wrapper>
      <button mat-flat-button color="primary" (click)="save(mds)">Test: Save</button>
    `,
    }),
    args: {
        embedded: true,
        setId: DEFAULT,
        editorMode: 'nodes',
        nodes: [DummyNode as Node].map((n) => {
            n = Helper.deepCopy(n);
            n.ref.id = Math.random() + '';
            delete n.properties['cclom:title'];
            delete n.properties['cclom:general_description'];
            console.log(n.properties);
            registerMockNode(n);
            return n;
        }),
    },
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<MdsEditorWrapperComponent>;
export const MdsIOTemplate: Story = {
    args: {
        groupId: 'io',
    },
};
export const MdsIOBulkTemplate: Story = {
    args: {
        groupId: 'io_bulk',
        nodes: [DummyNode as Node, DummyNode as Node].map((n, i) => {
            n = Helper.deepCopy(n);
            n.ref.id = Math.random() + '';
            n.properties['ccm:tool_category'] = ['communication'];
            n.properties['ccm:educationallearningresourcetype'] = [
                'figure',
                'diagram',
                'slide',
                'graph',
                'index',
                'narrativetext',
            ];
            n.properties['ccm:educationaltypicalagerange_from'] = ['10'];
            n.properties['ccm:educationaltypicalagerange_to'] = ['20'];
            const keywords = n.properties['cclom:general_keyword'];
            if (i == 1) {
                keywords.splice(1, 1);
                n.properties['ccm:taxonid'].splice(1, 3);
                n.properties['ccm:educationallearningresourcetype'].splice(1, 2);
                n.properties['cclom:title'] = ['Test 2'];
                // differing single value, rendered as "(different values)" by the select widget
                n.properties['ccm:tool_category'] = ['cooperation'];
            }
            registerMockNode(n);
            return n;
        }),
    },
    play: async ({ canvasElement }) => {
        // `cclom:title` and `ccm:tool_category` differ between both nodes, so the widgets must
        // show the "(different values)" hint - which requires a floating label to be visible,
        // see `alwaysFloatLabel` of the widget container.
        const title = await waitFor(() => getFormField(canvasElement, 'cclom:title'));
        await waitFor(() =>
            expect(title.querySelector('input')).toHaveAttribute(
                'placeholder',
                '(Verschiedene Werte)',
            ),
        );
        expect(title.querySelector('mat-label')).toHaveClass('mdc-floating-label--float-above');
        const category = await waitFor(() => getFormField(canvasElement, 'ccm:tool_category'));
        await waitFor(() => expect(category.textContent).toContain('(Verschiedene Werte)'));
        expect(category.querySelector('mat-label')).toHaveClass('mdc-floating-label--float-above');

        // A tree widget renders values that only some of the nodes carry as indeterminate chips.
        // Enabling the field and clicking such a chip confirms the value for all nodes.
        const taxonId = await waitFor(() => getWidgetContainer(canvasElement, 'ccm:taxonid'));
        await userEvent.click(within(taxonId).getByRole('switch'));
        const indeterminateChip = await waitFor(() => {
            const chip = taxonId.querySelector<HTMLElement>('mat-chip-row.indeterminate');
            expect(chip, 'indeterminate chip').toBeTruthy();
            return chip;
        });
        await userEvent.click(indeterminateChip);
        await waitFor(() => expect(taxonId.querySelector('mat-chip-row.indeterminate')).toBeNull());
    },
};

const FEEDBACK_OPTION = 'OPTIONS.MATERIAL_FEEDBACK';
const TEMPLATE_GROUP = 'storybook_template_features';

/**
 * View exercising the two template features that are resolved by `MdsEditorViewComponent`
 * itself: `<i18n …>` tags and the generic `<action>` widget.
 */
const TEMPLATE_FEATURES_HTML = `
    <h3><i18n MDS.LICENSE></h3>
    <p>Unbekannter Key bleibt stehen: <i18n MDS.NO_SUCH_KEY></p>
    <cclom:title>
    <action option="${FEEDBACK_OPTION}">
    <action option="${FEEDBACK_OPTION}" caption="Rückmeldung geben" icon="feedback">
    <action option="OPTIONS.NOT_AVAILABLE_FOR_THIS_NODE">
`;

const TemplateFeaturesMds: MdsDefinition = {
    ...DefaultMds,
    views: [
        ...DefaultMds.views,
        {
            id: TEMPLATE_GROUP,
            caption: 'Template-Features',
            icon: 'description',
            html: TEMPLATE_FEATURES_HTML,
            rel: null,
            hideIfEmpty: false,
            isExtended: false,
        },
    ],
    groups: [
        ...DefaultMds.groups,
        { id: TEMPLATE_GROUP, views: [TEMPLATE_GROUP], rendering: 'angular' },
    ],
};

@Injectable()
class TemplateFeaturesMdsService {
    getMetadataSet(_identifier: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return of(TemplateFeaturesMds);
    }
}

/** only `OPTIONS.MATERIAL_FEEDBACK` is available, so the third `<action>` must stay invisible */
@Injectable()
class OptionsHelperDataServiceMock {
    async getAvailableOptions(_target: Target): Promise<OptionItem[]> {
        return [
            new OptionItem(FEEDBACK_OPTION, 'chat_bubble', () => alert('Test: option triggered')),
        ];
    }
}

/**
 * `<i18n KEY>` is replaced with the frontend translation before the template html is parsed,
 * and `<action option="…">` renders the matching option of the options helper.
 */
export const TemplateFeatures: Story = {
    decorators: [
        applicationConfig({
            providers: [
                { provide: MdsService, useClass: TemplateFeaturesMdsService },
                { provide: OptionsHelperDataService, useClass: OptionsHelperDataServiceMock },
            ],
        }),
    ],
    args: {
        groupId: TEMPLATE_GROUP,
        editorMode: 'viewer',
    },
};

const SUGGESTIONS_GROUP = 'storybook_suggestions';
const SUGGESTIONS_VIEW = 'storybook_suggestions_view';
const SUGGESTION_WIDGET = 'cclom:general_keyword';

/**
 * Renders the same widget twice: as a regular (primary) widget and, in a view with
 * `rel="suggestions"`, as `es-mds-editor-widget-suggestion-chips`. The suggestion values are
 * served by `SearchServiceMock.observeFacet`.
 */
const SuggestionsMds: MdsDefinition = {
    ...DefaultMds,
    views: [
        ...DefaultMds.views,
        {
            id: SUGGESTIONS_GROUP,
            caption: 'Schlagworte',
            icon: 'label',
            html: `<${SUGGESTION_WIDGET}>`,
            rel: null,
            hideIfEmpty: false,
            isExtended: false,
        },
        {
            id: SUGGESTIONS_VIEW,
            caption: 'Vorschläge',
            icon: 'lightbulb_outline',
            html: `<${SUGGESTION_WIDGET}>`,
            rel: 'suggestions',
            hideIfEmpty: false,
            isExtended: false,
        },
    ],
    groups: [
        ...DefaultMds.groups,
        {
            id: SUGGESTIONS_GROUP,
            views: [SUGGESTIONS_GROUP, SUGGESTIONS_VIEW],
            rendering: 'angular',
        },
    ],
};

@Injectable()
class SuggestionsMdsService {
    getMetadataSet(_identifier: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return of(SuggestionsMds);
    }
}

/**
 * Clicking a suggestion chip has to add the value to the primary widget - which only works if
 * that widget listens to `Widget.addValue` (the suggestion widget has no other way to reach it).
 */
export const SuggestionChips: Story = {
    decorators: [
        applicationConfig({
            providers: [{ provide: MdsService, useClass: SuggestionsMdsService }],
        }),
    ],
    args: {
        groupId: SUGGESTIONS_GROUP,
    },
    play: async ({ canvasElement }) => {
        const canvas = within(canvasElement);
        const suggestion = await canvas.findByText('Physik');
        await userEvent.click(suggestion);
        const primaryWidget = await waitFor(() => getFormField(canvasElement, SUGGESTION_WIDGET));
        await waitFor(() => expect(within(primaryWidget).getByText('Physik')).toBeTruthy());
    },
};
