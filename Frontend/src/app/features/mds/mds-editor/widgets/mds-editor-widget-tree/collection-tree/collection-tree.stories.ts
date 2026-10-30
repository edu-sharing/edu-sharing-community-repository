import { MatSnackBarModule } from '@angular/material/snack-bar';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { InitialValues } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../../shared/shared.module';
import { MdsWidget, RequiredMode } from '../../../../types/types';
import { Widget } from '../../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../../mds-editor-widget-container/register-form-field.directive';
import { HighlightPipe } from '../mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetTreeCoreComponent } from '../mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { MdsEditorWidgetTreeComponent } from '../mds-editor-widget-tree.component';
import { CollectionScopesService } from '../../../../../../services/collection-scopes.service';
import {
    CollectionScopesServiceMock,
    CollectionTreeServiceMock,
} from './collection-tree-storybook-mock';
import { CollectionTreeService } from './collection-tree.service';

/** A widget whose initial values are given by the story. */
class CollectionTreeWidgetDummy extends WidgetDummy {
    constructor(definition: MdsWidget, private readonly initialIds: string[]) {
        super(definition);
        // collection trees filter searches, which list no proposals
        this.setSuggestions([]);
    }

    getInitalValuesAsync(): Promise<InitialValues> {
        return Promise.resolve({ jointValues: this.initialIds, individualValues: [] });
    }
}

type StoryArgs = MdsWidget & { initialIds?: string[] };

const meta: Meta<StoryArgs> = {
    title: 'Mds/Widget/Tree',
    component: MdsEditorWidgetTreeComponent,
    decorators: [
        moduleMetadata({
            declarations: [
                MdsEditorWidgetContainerComponent,
                MdsEditorWidgetTreeCoreComponent,
                HighlightPipe,
                RegisterFormFieldDirective,
            ],
            imports: [MatSnackBarModule, SharedModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                { provide: CollectionTreeService, useClass: CollectionTreeServiceMock },
                { provide: CollectionScopesService, useClass: CollectionScopesServiceMock },
            ],
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        type: { table: { disable: true } },
        caption: { control: 'text' },
        bottomCaption: { control: 'text' },
        configuration: { control: 'text' },
        initialIds: { control: 'object' },
        isRequired: {
            control: 'inline-radio',
            options: [RequiredMode.Mandatory, RequiredMode.Optional, RequiredMode.Ignore],
        },
    },
    args: {
        id: 'virtual:collection_id_tree',
        type: 'collectionTree',
        caption: 'Sammlung',
        bottomCaption: 'Enthält jeweils alle Unterebenen',
        isRequired: RequiredMode.Optional,
        initialIds: [],
    },
    tags: ['autodocs'],
    render: ({ initialIds, ...definition }) => ({
        props: {
            widget: new CollectionTreeWidgetDummy(
                definition,
                initialIds ?? [],
            ) as unknown as Widget,
        },
    }),
};

export default meta;
type Story = StoryObj<StoryArgs>;

/**
 * Starts at "Fächer", with one selected collection outside of it. "Englisch" holds more
 * collections than fit on one page. Without a configuration, the categories of the collections
 * page are listed instead.
 */
export const CollectionTree: Story = {
    args: {
        configuration: '{"rootCollectionId":"subjects"}',
        initialIds: ['optics', 'mine'],
    },
};
