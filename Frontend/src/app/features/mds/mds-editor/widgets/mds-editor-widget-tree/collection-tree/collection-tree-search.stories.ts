import { Injectable } from '@angular/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { DEFAULT, MdsDefinition, MdsIdentifier, MdsService } from 'ngx-edu-sharing-api';
import { Values } from 'ngx-edu-sharing-ui';
import { Observable, of } from 'rxjs';
import { SharedModule } from '../../../../../../shared/shared.module';
import { MdsModule } from '../../../../mds.module';
import { RequiredMode } from '../../../../types/types';
import { MdsEditorWrapperComponent } from '../../../mds-editor-wrapper/mds-editor-wrapper.component';
import { DefaultMds, mdsStorybookProviders } from '../../../storybook-utils';
import {
    COLLECTION_TREE_PRIMARY_PROPERTY,
    COLLECTION_TREE_PROPERTY,
} from './collection-tree-config';
import { CollectionTreeServiceMock } from './collection-tree-storybook-mock';
import { CollectionTreeService } from './collection-tree.service';

const SEARCH_GROUP = 'storybook_collection_tree_search';

/** Search form holding a collection tree between two other fields, showing its spacing. */
const SearchMds: MdsDefinition = {
    ...DefaultMds,
    widgets: [
        ...DefaultMds.widgets,
        {
            ...DefaultMds.widgets[0],
            ids: null,
            id: COLLECTION_TREE_PROPERTY,
            caption: 'Sammlung',
            bottomCaption: null,
            placeholder: null,
            type: 'collectionTree',
            configuration: '{"rootCollectionId":"subjects"}',
            isRequired: RequiredMode.Optional,
        },
    ],
    views: [
        ...DefaultMds.views,
        {
            id: SEARCH_GROUP,
            caption: 'Suche',
            icon: 'search',
            html: `<cclom:title><${COLLECTION_TREE_PROPERTY}><ccm:educationallearningresourcetype>`,
            rel: null,
            hideIfEmpty: false,
            isExtended: false,
        },
    ],
    groups: [
        ...DefaultMds.groups,
        { id: SEARCH_GROUP, views: [SEARCH_GROUP], rendering: 'angular' },
    ],
};

@Injectable()
class SearchMdsService {
    getMetadataSet(_identifier: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return of(SearchMds);
    }
}

const meta: Meta<MdsEditorWrapperComponent> = {
    title: 'Mds/Widget/Collection Tree in Search',
    component: MdsEditorWrapperComponent,
    decorators: [
        moduleMetadata({ imports: [MdsModule, SharedModule] }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                { provide: CollectionTreeService, useClass: CollectionTreeServiceMock },
                { provide: MdsService, useClass: SearchMdsService },
            ],
        }),
    ],
};

export default meta;
type Story = StoryObj<MdsEditorWrapperComponent>;

/** Renders the search form starting with the given values and lists the values it emits. */
function renderSearchForm(initialValues: Values): Story['render'] {
    return () => ({
        props: {
            setId: DEFAULT,
            groupId: SEARCH_GROUP,
            initialValues,
            values: null as Values,
            // the listener re-renders the story once the editor has loaded, which zoneless
            // change detection does not do for the editor's own loading state
            loaded: false,
        },
        template: `
      <es-mds-editor-wrapper
        [embedded]="true"
        [setId]="setId"
        [groupId]="groupId"
        [currentValues]="initialValues"
        editorMode="search"
        (currentValuesChange)="values = $event"
        (mdsLoaded)="loaded = true"
      ></es-mds-editor-wrapper>
      <h4>Search values</h4>
      <pre data-testid="search-values">{{ values | json }}</pre>
    `,
    });
}

/**
 * The tree inside a search form. Below the form are the search values it emits: the picked
 * collections move to `virtual:collection_id_primary` once sub-collections are switched off.
 */
export const SearchValues: Story = {
    render: renderSearchForm({}),
};

/** Starts from values that leave out sub-collections, as a search link or a teaser stores them. */
export const RestoredWithoutSubcollections: Story = {
    render: renderSearchForm({ [COLLECTION_TREE_PRIMARY_PROPERTY]: ['optics'] }),
};
