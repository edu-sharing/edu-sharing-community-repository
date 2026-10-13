import { Injectable } from '@angular/core';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { HOME_REPOSITORY, Node } from 'ngx-edu-sharing-api';
import { InitialValues } from 'ngx-edu-sharing-ui';
import { Observable, of, throwError, timer } from 'rxjs';
import { delay, switchMap } from 'rxjs/operators';
import { SharedModule } from '../../../../../../shared/shared.module';
import { MdsWidget, RequiredMode } from '../../../../types/types';
import { Widget } from '../../../mds-editor-instance.service';
import { mdsStorybookProviders, WidgetDummy } from '../../../storybook-utils';
import { MdsEditorWidgetContainerComponent } from '../../mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from '../../mds-editor-widget-container/register-form-field.directive';
import { HighlightPipe } from '../mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetTreeCoreComponent } from '../mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { MdsEditorWidgetTreeComponent } from '../mds-editor-widget-tree.component';
import { CollectionTreeScope } from './collection-tree-config';
import {
    CollectionPage,
    CollectionTreeService,
    getParentIdOf,
    PARENT_ID_PROPERTY,
    PlacedCollection,
} from './collection-tree.service';

interface FixtureCollection {
    id: string;
    title: string;
    parent: string | null;
    /** Categories a top-level collection is listed in. */
    scopes?: CollectionTreeScope[];
}

const RESPONSE_DELAY_MS = 400;

const FIXTURE: FixtureCollection[] = [
    // both mine and editorial, so it is listed under both categories
    { id: 'subjects', title: 'Fächer', parent: null, scopes: ['MY', 'TYPE_EDITORIAL'] },
    {
        id: 'foreign',
        title: 'Öffentliche Sammlung einer anderen Redaktion',
        parent: null,
        scopes: ['EDU_ALL'],
    },
    { id: 'foreign-physics', title: 'Physik-Sammlung einer anderen Redaktion', parent: 'foreign' },
    { id: 'mine', title: 'Meine Sammlung', parent: null, scopes: ['MY'] },
    { id: 'math', title: 'Mathematik', parent: 'subjects' },
    // the same title in two subjects, told apart by their location in the search
    { id: 'math-methods', title: 'Methoden', parent: 'math' },
    { id: 'physics-methods', title: 'Methoden', parent: 'physics' },
    { id: 'algebra', title: 'Algebra', parent: 'math' },
    { id: 'geometry', title: 'Geometrie', parent: 'math' },
    { id: 'physics', title: 'Physik', parent: 'subjects' },
    { id: 'mechanics', title: 'Mechanik', parent: 'physics' },
    { id: 'kinematics', title: 'Kinematik', parent: 'mechanics' },
    { id: 'optics', title: 'Optik', parent: 'physics' },
    { id: 'chemistry', title: 'Chemie', parent: 'subjects' },
    { id: 'english', title: 'Englisch', parent: 'subjects' },
    // more topics than fit on one page
    ...Array.from({ length: 60 }, (_, i) => ({
        id: `english-topic-${i + 1}`,
        title: `Englisch – Thema ${i + 1}`,
        parent: 'english',
    })),
];

function toNode(collection: FixtureCollection): Node {
    return {
        ref: { id: collection.id, repo: HOME_REPOSITORY, archived: false, isHomeRepo: true },
        name: collection.title,
        title: collection.title,
        type: 'ccm:map',
        aspects: ['ccm:collection'],
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
    getAncestors(id: string): Observable<Node[]> {
        const path = getPath(id);
        if (!path.length) {
            return timer(RESPONSE_DELAY_MS).pipe(
                switchMap(() => throwError(() => new Error(`${id} not found`))),
            );
        }
        return of(path.map(toNode)).pipe(delay(RESPONSE_DELAY_MS));
    }

    getWithAncestors(nodes: Node[]): Observable<Map<string, PlacedCollection>> {
        const placed = new Map<string, PlacedCollection>();
        for (const collection of nodes.flatMap((node) => getPath(node.ref.id))) {
            const node = toNode(collection);
            placed.set(collection.id, { node, parentId: getParentIdOf(node) });
        }
        return of(placed).pipe(delay(RESPONSE_DELAY_MS));
    }

    hasCollections(scope: CollectionTreeScope): Observable<boolean> {
        const filled = FIXTURE.some((collection) => collection.scopes?.includes(scope));
        return of(filled).pipe(delay(RESPONSE_DELAY_MS));
    }

    getChildren(
        parent: Node | null,
        options: { scope?: CollectionTreeScope; skipCount: number; maxItems: number },
    ): Observable<CollectionPage> {
        const parentId = parent?.ref.id ?? null;
        const children = FIXTURE.filter(
            (collection) =>
                collection.parent === parentId &&
                (parentId || collection.scopes?.includes(options.scope)),
        );
        const nodes = children
            .slice(options.skipCount, options.skipCount + options.maxItems)
            .map(toNode);
        return of({ nodes, total: children.length }).pipe(delay(RESPONSE_DELAY_MS));
    }

    searchCollections(
        term: string,
        options: { rootId: string | null; maxItems: number },
    ): Observable<Node[]> {
        const words = term.toLowerCase().split(/\s+/);
        const hits = FIXTURE.filter(
            (collection) =>
                words.every((word) => collection.title.toLowerCase().includes(word)) &&
                (!options.rootId ||
                    getPath(collection.id)
                        .slice(0, -1)
                        .some((ancestor) => ancestor.id === options.rootId)),
        );
        return of(hits.slice(0, options.maxItems).map(toNode)).pipe(delay(RESPONSE_DELAY_MS));
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
