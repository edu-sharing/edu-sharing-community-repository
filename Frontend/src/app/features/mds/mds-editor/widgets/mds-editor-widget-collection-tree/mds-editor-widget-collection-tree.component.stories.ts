import { Injectable } from '@angular/core';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { HOME_REPOSITORY, Node } from 'ngx-edu-sharing-api';
import { InitialValues } from 'ngx-edu-sharing-ui';
import { Observable, of, throwError, timer } from 'rxjs';
import { delay, switchMap } from 'rxjs/operators';
import { SharedModule } from '../../../../../shared/shared.module';
import { MdsWidget, RequiredMode } from '../../../types/types';
import { Widget } from '../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../storybook-utils';
import { HighlightPipe } from '../mds-editor-widget-tree/mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetContainerComponent } from '../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../mds-editor-widget-container/register-form-field.directive';
import { CollectionTreeScope } from './collection-tree-config';
import {
    CollectionPage,
    CollectionTreeService,
    PARENT_ID_PROPERTY,
} from './collection-tree.service';
import { MdsEditorWidgetCollectionTreeComponent } from './mds-editor-widget-collection-tree.component';
import { MdsEditorWidgetCollectionTreeCoreComponent } from './mds-editor-widget-collection-tree-core/mds-editor-widget-collection-tree-core.component';

interface FixtureCollection {
    id: string;
    title: string;
    parent: string | null;
    writable?: boolean;
    /** Categories a top-level collection is listed in. */
    scopes?: CollectionTreeScope[];
}

/** Id of a collection whose sub-collections fail to load on the first attempt. */
const FAILING_COLLECTION_ID = 'unstable';
const RESPONSE_DELAY_MS = 400;

const FIXTURE: FixtureCollection[] = [
    // both mine and editorial, so it is listed under both categories
    {
        id: 'subjects',
        title: 'Fächer',
        parent: null,
        writable: true,
        scopes: ['MY', 'TYPE_EDITORIAL'],
    },
    {
        id: 'foreign',
        title: 'Öffentliche Sammlung einer anderen Redaktion',
        parent: null,
        scopes: ['EDU_ALL'],
    },
    { id: 'foreign-physics', title: 'Physik-Sammlung einer anderen Redaktion', parent: 'foreign' },
    { id: 'mine', title: 'Meine Sammlung', parent: null, writable: true, scopes: ['MY'] },
    { id: 'math', title: 'Mathematik', parent: 'subjects', writable: true },
    // the same title in two subjects, told apart by their location in the search
    { id: 'math-methods', title: 'Methoden', parent: 'math', writable: true },
    { id: 'physics-methods', title: 'Methoden', parent: 'physics', writable: true },
    { id: 'algebra', title: 'Algebra', parent: 'math', writable: true },
    { id: 'geometry', title: 'Geometrie', parent: 'math', writable: true },
    { id: 'physics', title: 'Physik', parent: 'subjects', writable: true },
    { id: 'mechanics', title: 'Mechanik', parent: 'physics', writable: true },
    { id: 'kinematics', title: 'Kinematik', parent: 'mechanics', writable: true },
    { id: 'optics', title: 'Optik', parent: 'physics' },
    { id: 'chemistry', title: 'Chemie', parent: 'subjects' },
    { id: FAILING_COLLECTION_ID, title: 'Instabile Sammlung', parent: 'subjects', writable: true },
    { id: 'stable', title: 'Stabile Untersammlung', parent: FAILING_COLLECTION_ID },
    { id: 'english', title: 'Englisch', parent: 'subjects', writable: true },
    // more topics than fit on one page
    ...Array.from({ length: 60 }, (_, i) => ({
        id: `english-topic-${i + 1}`,
        title: `Englisch – Thema ${i + 1}`,
        parent: 'english',
        writable: i % 3 !== 0,
    })),
];

function toNode(collection: FixtureCollection): Node {
    return {
        ref: { id: collection.id, repo: HOME_REPOSITORY, archived: false, isHomeRepo: true },
        name: collection.title,
        title: collection.title,
        type: 'ccm:map',
        aspects: ['ccm:collection'],
        access: collection.writable ? ['Read', 'Write'] : ['Read'],
        properties: collection.parent ? { [PARENT_ID_PROPERTY]: [collection.parent] } : {},
        collection: {
            childCollectionsCount: FIXTURE.filter((child) => child.parent === collection.id).length,
            level0: !collection.parent,
        },
    } as unknown as Node;
}

function getPath(id: string): FixtureCollection[] {
    const path: FixtureCollection[] = [];
    let current = FIXTURE.find((collection) => collection.id === id);
    while (current) {
        path.unshift(current);
        current = FIXTURE.find((collection) => collection.id === current.parent);
    }
    return path;
}

/** Serves {@link FIXTURE} with a short delay so loading states are visible. */
@Injectable()
class CollectionTreeServiceMock implements Partial<CollectionTreeService> {
    private failedOnce = false;

    getAncestors(id: string): Observable<Node[]> {
        const path = getPath(id);
        if (!path.length) {
            return timer(RESPONSE_DELAY_MS).pipe(
                switchMap(() => throwError(() => new Error(`${id} not found`))),
            );
        }
        return of(path.map(toNode)).pipe(delay(RESPONSE_DELAY_MS));
    }

    getCollections(ids: string[]): Observable<Node[]> {
        const nodes = FIXTURE.filter((collection) => ids.includes(collection.id)).map(toNode);
        return of(nodes).pipe(delay(RESPONSE_DELAY_MS));
    }

    getChildren(
        parent: Node | null,
        options: { scope?: CollectionTreeScope; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
        const parentId = parent?.ref.id ?? null;
        if (parentId === FAILING_COLLECTION_ID && !this.failedOnce) {
            this.failedOnce = true;
            return timer(RESPONSE_DELAY_MS).pipe(
                switchMap(() => throwError(() => new Error('temporarily unavailable'))),
            );
        }
        const children = FIXTURE.filter(
            (collection) =>
                collection.parent === parentId &&
                (parentId || collection.scopes?.includes(options.scope)),
        );
        return this.page(children, options);
    }

    searchCollections(
        term: string,
        options: { rootId: string | null; mdsId: string; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
        const words = term.toLowerCase().split(/\s+/);
        const hits = FIXTURE.filter(
            (collection) =>
                words.every((word) => collection.title.toLowerCase().includes(word)) &&
                (!options.rootId ||
                    getPath(collection.id)
                        .slice(0, -1)
                        .some((ancestor) => ancestor.id === options.rootId)),
        );
        return this.page(hits, options);
    }

    private page(
        collections: FixtureCollection[],
        options: { skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
        const nodes = collections
            .slice(options.skipCount, options.skipCount + options.maxItems)
            .map(toNode);
        return of({ nodes, total: collections.length }).pipe(delay(RESPONSE_DELAY_MS));
    }
}

/** A widget whose initial values are given by the story. */
class CollectionTreeWidgetDummy extends WidgetDummy {
    constructor(definition: MdsWidget, private readonly initialIds: string[]) {
        super(definition);
    }

    getInitalValuesAsync(): Promise<InitialValues> {
        return Promise.resolve({ jointValues: this.initialIds, individualValues: [] });
    }
}

type StoryArgs = MdsWidget & { initialIds?: string[] };

const meta: Meta<StoryArgs> = {
    title: 'Mds/Widget/CollectionTree',
    component: MdsEditorWidgetCollectionTreeComponent,
    decorators: [
        moduleMetadata({
            declarations: [
                MdsEditorWidgetContainerComponent,
                MdsEditorWidgetCollectionTreeCoreComponent,
                HighlightPipe,
                RegisterFormFieldDirective,
            ],
            imports: [MatSnackBarModule, SharedModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                { provide: CollectionTreeService, useClass: CollectionTreeServiceMock },
            ],
        }),
    ],
    argTypes: {
        id: { table: { disable: true } },
        type: { table: { disable: true } },
        caption: { control: 'text' },
        bottomCaption: { control: 'text' },
        placeholder: { control: 'text' },
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
        placeholder: 'Sammlung auswählen',
        bottomCaption: 'Enthält jeweils alle Unterebenen',
        isRequired: RequiredMode.Optional,
        initialIds: [],
    },
    tags: ['autodocs'],
    render: ({ initialIds, ...definition }) => {
        const widget = new CollectionTreeWidgetDummy(
            definition,
            initialIds ?? [],
        ) as unknown as Widget;
        (window as any).widget = widget;
        return {
            props: {
                widget,
            },
        };
    },
};

export default meta;
type Story = StoryObj<StoryArgs>;

/** Without a start collection, the top level is listed like on the collections page. */
export const TopLevel: Story = {};

/**
 * Starts at "Fächer", with one selected collection outside of it. "Englisch" holds more
 * collections than fit on one page, "Instabile Sammlung" fails to load on the first attempt.
 */
export const StartCollection: Story = {
    args: {
        configuration: '{"rootCollectionId":"subjects"}',
        initialIds: ['optics', 'mine'],
    },
};
