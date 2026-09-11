import { Injectable, inject } from '@angular/core';
import { CollectionEntries, Node } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';
import { GridTile } from '../types/grid-tile';
import { PageStructure } from '../types/page-structure';
import { RepeatItem, SwimlaneRepeat } from '../types/swimlane-repeat';
import { Swimlane } from '../types/swimlane';
import { interpolate } from '../utils/interpolate-util';
import { prependWorkspacePrefix, retrieveNodeId } from '../utils/template-util';
import { TopicPageHelperService } from './topic-page-helper.service';

/**
 * The page a repeat rule is resolved for. Its fields are addressable as `${page.<field>}`.
 */
export interface RepeatPageContext {
    collectionId: string;
    title?: string;
}

type RepeatSourceResolver = (
    repeat: SwimlaneRepeat,
    page: RepeatPageContext,
) => Promise<RepeatItem[]>;

/**
 * Expands the repeat rules of a page template into plain swimlanes.
 *
 * A page template may carry swimlanes that stand for a number of swimlanes rather than for one —
 * see `SwimlaneRepeat`. This service turns such a rule into its result, which is what gets
 * persisted on a page variant: ordinary swimlanes that carry no trace of the rule and stay
 * editable like hand-built ones.
 */
@Injectable({
    providedIn: 'root',
})
export class SwimlaneRepeatService {
    private topicPageHelperService = inject(TopicPageHelperService);

    private readonly resolvers: Map<string, RepeatSourceResolver> = new Map([
        ['subCollections', (repeat, page) => this.resolveSubCollections(repeat, page)],
    ]);

    /**
     * Replaces every swimlane carrying a repeat rule by one swimlane per resolved item, in place.
     * A rule that resolves to nothing leaves no swimlane behind, and a source without a resolver
     * leaves the swimlane as it is so an unknown rule cannot empty a page.
     *
     * @param structure
     * @param page
     */
    async expandRepeats(
        structure: PageStructure,
        page: RepeatPageContext,
        options: { preview?: boolean } = {},
    ): Promise<void> {
        if (!structure?.swimlanes?.length) {
            return;
        }
        const expanded: Swimlane[] = [];
        for (const swimlane of structure.swimlanes) {
            if (!swimlane.repeat) {
                expanded.push(swimlane);
                continue;
            }
            const resolver: RepeatSourceResolver = this.resolvers.get(swimlane.repeat.source);
            if (!resolver) {
                console.warn('unknown swimlane repeat source', swimlane.repeat.source);
                expanded.push(swimlane);
                continue;
            }
            const items: RepeatItem[] = await resolver(swimlane.repeat, page);
            expanded.push(
                ...items.map((item, index) =>
                    this.buildSwimlane(swimlane, item, index, page, options.preview),
                ),
            );
        }
        structure.swimlanes = expanded;
    }

    /**
     * Resolves the repeat rules into swimlanes meant for rendering only, leaving the structure
     * untouched. A tile's config patch becomes the config it renders from, so a preview shows
     * what the rule produces without anything being created.
     *
     * @param structure
     * @param page
     */
    async expandForPreview(structure: PageStructure, page: RepeatPageContext): Promise<Swimlane[]> {
        const preview: PageStructure = {
            ...structure,
            swimlanes: JSON.parse(JSON.stringify(structure?.swimlanes ?? [])) as Swimlane[],
        };
        await this.expandRepeats(preview, page, { preview: true });
        // a patch outside a repeat has no item to address and would render its placeholders
        // verbatim, so it is dropped rather than applied
        preview.swimlanes.forEach((swimlane: Swimlane): void =>
            swimlane.grid?.forEach((gridTile: GridTile): void => {
                delete gridTile.configPatch;
            }),
        );
        return preview.swimlanes;
    }

    /**
     * Builds one concrete swimlane for a resolved item. The grid tiles keep their `nodeId`, so the
     * template's widget node is copied once per item by the regular widget copy step.
     *
     * @param template
     * @param item
     * @param index
     * @param page
     */
    private buildSwimlane(
        template: Swimlane,
        item: RepeatItem,
        index: number,
        page: RepeatPageContext,
        preview: boolean = false,
    ): Swimlane {
        const swimlane: Swimlane = interpolate(template, { item, index, page });
        swimlane.id = uuidv4();
        delete swimlane.repeat;
        if (preview) {
            // a previewed tile has no widget node, so it renders from its resolved patch
            swimlane.grid?.forEach((gridTile: GridTile): void => {
                if (gridTile.configPatch) {
                    gridTile.configOverwrite = JSON.stringify(gridTile.configPatch);
                    delete gridTile.configPatch;
                }
            });
        }
        return swimlane;
    }

    /**
     * Resolves the direct sub collections of the page's collection.
     *
     * @param repeat
     * @param page
     */
    private async resolveSubCollections(
        repeat: SwimlaneRepeat,
        page: RepeatPageContext,
    ): Promise<RepeatItem[]> {
        if (!page?.collectionId) {
            return [];
        }
        const entries: CollectionEntries = await firstValueFrom(
            this.topicPageHelperService.getSubcollections(page.collectionId, false, {
                orderBy: repeat.orderBy,
                ascending: !repeat.orderDescending,
                maxItems: repeat.maxItems,
            }),
        );
        return (entries?.collections ?? []).map((collection: Node) => ({
            nodeId: retrieveNodeId(collection),
            ref: prependWorkspacePrefix(retrieveNodeId(collection)),
            title: collection.title || collection.name,
            description: collection.collection?.description,
        }));
    }
}
