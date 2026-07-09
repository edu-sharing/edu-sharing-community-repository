import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { Node, StatisticV1Service, Usage, UsageV1Service } from 'ngx-edu-sharing-api';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { forkJoin } from 'rxjs';
import { RestConnectorService, RestConstants } from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UsageStatCardComponent } from './usage-stat-card.component';

/** A single usage metric shown as a (selectable) card */
export interface UsageMetric {
    id: string;
    icon: string;
    labelKey: string;
    value: number;
    /** work-in-progress metric: not clickable, shows a "coming soon" badge */
    comingSoon?: boolean;
    /** disabled metric: not clickable, dimmed */
    disabled?: boolean;
}

/**
 * A collection usage as returned by `/usages/node/{nodeId}/collections`.
 * (The generated client types this loosely as `Collection[]`,
 * the runtime, however, carries the collection node under `collection`.)
 */
export interface CollectionUsageEntry {
    collection: Node;
    collectionUsageType?: string;
}

/** Predefined date range presets, expressed in days */
const RANGE_OPTIONS = [7, 30, 90, 365] as const;

/** Tracking action keys later mapped to the metric cards */
const ACTION_VIEW = 'VIEW_MATERIAL';
const ACTION_VIEW_EMBEDDED = 'VIEW_MATERIAL_EMBEDDED';
const ACTION_DOWNLOAD = 'DOWNLOAD_MATERIAL';
const ACTION_PLAY = 'VIEW_MATERIAL_PLAY_MEDIA';

/** Display names for the supported embedding platforms */
const PLATFORM_NAMES = {
    wordpress: 'WordPress',
    moodle: 'Moodle',
    ilias: 'ILIAS',
};

/** Platform types relevant for the embedding table */
export type EmbeddingPlatform = keyof typeof PLATFORM_NAMES;

/** The generic detail table for a selected metric */
export interface UsageDetailTable {
    titleKey: string;
    /** first column header (e.g., course/folder or school/organization) */
    columnKey: string;
    /** second column header; omit for a list without a value column */
    valueColumnKey?: string;
    rows: UsageDetailRow[];
    emptyKey: string;
    /** total row label; omit to render no total row */
    totalLabelKey?: string;
    total?: number;
}

/** One row in the generic detail table (shared by all metric detail views) */
export interface UsageDetailRow {
    /** stable key for `@for` tracking */
    key: string;
    /** main text of the row (fallback when `node` is not set) */
    label: string;
    /** cluster size badge – number of usages collapsed into this row (>1 only) */
    badge?: number;
    /** node whose title is rendered via the `nodeTitle` pipe (collections) */
    node?: Node;
    /** routerLink commands – renders the row title as a real link when set */
    link?: unknown[];
    /** query params for the link */
    queryParams?: { [key: string]: string };
    /** brand image icon URL (embeddings); `icon` is used when not set */
    iconUrl?: string;
    /** material icon name (used when `iconUrl` is not set) */
    icon?: string;
    /** numeric value shown in the value column (views/downloads/...) */
    value?: number;
    /** preformatted percentage label, e.g. "24,8 %" */
    percentLabel?: string;
    /** bar fill width in percent (0–100), relative to the largest row */
    barPercent?: number;
}

@Component({
    selector: 'es-usages-preview',
    templateUrl: 'usages-preview.component.html',
    styleUrls: ['usages-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [SharedModule, UsageStatCardComponent],
})
export class UsagesPreviewComponent {
    private connector = inject(RestConnectorService);
    private nodeHelper = inject(NodeHelperService);
    private router = inject(Router);
    private statisticApi = inject(StatisticV1Service);
    private usageApi = inject(UsageV1Service);

    /** One or more nodes whose usages are displayed */
    readonly nodes = input<Node[]>([]);

    /** Ids of the selected nodes (null-safe: the binding may pass null) */
    private readonly nodeIds = computed(() => (this.nodes() ?? []).map((node) => node.ref.id));
    /** Stable key of the current selection (used for out-of-order response guards) */
    private readonly selectionKey = computed(() => this.nodeIds().join(','));
    /**
     * Whether the statistics preview should be shown. Folders and
     * collections have no preview – only a hint pointing to the dashboard is shown.
     */
    protected readonly showStatisticsPreview = computed(
        () =>
            !(this.nodes() ?? []).some(
                (node) =>
                    !!node &&
                    (node.mediatype === 'folder' || this.nodeHelper.isNodeCollection(node)),
            ),
    );
    /**
     * Whether more than one node is selected. The embedding / usage details are retrieved
     * per-node and should not be aggregated, so it is disabled for a multi-selection.
     */
    protected readonly isMultiSelection = computed(() => this.nodeIds().length > 1);

    protected readonly rangeOptions = RANGE_OPTIONS;
    protected readonly selectedRange = signal<number>(RANGE_OPTIONS[0]);

    /** External feedback link shown in a container below the details */
    protected readonly feedbackUrl = 'https://edu-sharing.com/impressum/';

    /** Whether the dashboard link is shown – requires the selective-statistics toolpermission */
    protected readonly showDashboardLink = this.connector.hasToolPermissionInstant(
        RestConstants.TOOLPERMISSION_SELECTIVE_STATISTICS_NODES,
    );

    /** Views = direct material views + embedded views */
    private readonly viewCount = signal(0);
    /** Embedded views only (VIEW_MATERIAL_EMBEDDED) – used as the embeddings table total */
    private readonly embeddedViewCount = signal(0);
    private readonly downloadCount = signal(0);
    private readonly playCount = signal(0);
    /** LMS/CMS embeddings (for the embedding details) */
    protected readonly embeddings = signal<Usage[]>([]);
    /** Collection usages (for the embedding details) */
    private readonly collectionUsages = signal<CollectionUsageEntry[]>([]);
    /** Only embeddings on supported platforms are relevant */
    private readonly embeddingUsages = computed(() =>
        this.embeddings().filter((usage) => embeddingPlatform(usage) != null),
    );
    /** Embedding / usage count */
    private readonly embeddingCount = computed(
        () => this.embeddingUsages().length + this.collectionUsages().length,
    );

    /** Node selection key the statistics currently reflect */
    private lastStatsKey: string | null = null;

    /** The four metrics rendered as cards */
    protected readonly metrics = computed<UsageMetric[]>(() => [
        {
            id: 'view',
            icon: 'visibility',
            labelKey: 'WORKSPACE.METADATA.USAGE_TYPE.VIEW',
            value: this.viewCount(),
            // detail view still work in progress
            comingSoon: true,
        },
        {
            id: 'download',
            icon: 'file_download',
            labelKey: 'WORKSPACE.METADATA.USAGE_TYPE.DOWNLOAD',
            value: this.downloadCount(),
            comingSoon: true,
        },
        {
            id: 'play',
            icon: 'play_arrow',
            labelKey: 'WORKSPACE.METADATA.USAGE_TYPE.PLAY',
            value: this.playCount(),
            comingSoon: true,
        },
        {
            id: 'embedding',
            icon: 'code',
            labelKey: 'WORKSPACE.METADATA.USAGE_TYPE.USED',
            value: this.embeddingCount(),
            // per-node lookup: not meaningful across a multi-selection
            disabled: this.isMultiSelection(),
        },
    ]);

    /** Currently selected metric id (embeddings is the only interactive metric) */
    private readonly selectedMetricId = signal<string>('embedding');

    /** The currently selected metric (undefined if its id is not in the list) */
    protected readonly selectedMetric = computed(() =>
        this.metrics().find((metric) => metric.id === this.selectedMetricId()),
    );

    /** Compute the LMS/CMS embeddings table. */
    private readonly embeddingsTable = computed<UsageDetailTable | null>(() => {
        const usages = this.embeddingUsages();
        if (!usages.length) {
            return null;
        }
        return {
            titleKey: 'WORKSPACE.METADATA.USAGES.EMBEDDINGS_TITLE',
            columnKey: 'WORKSPACE.METADATA.USAGES.EMBEDDINGS_COLUMN',
            emptyKey: 'WORKSPACE.METADATA.USAGES.NO_EMBEDDINGS',
            totalLabelKey: 'WORKSPACE.METADATA.USAGES.EMBEDDINGS_TOTAL',
            // total embedding views (VIEW_MATERIAL_EMBEDDED)
            total: this.embeddedViewCount(),
            // usages from the same course (same appId + courseId) are clustered into one row
            rows: clusterUsagesByCourse(usages).map((group) => {
                const usage = group[0];
                // non-null: usages are pre-filtered to supported platforms (embeddingUsages)
                const platform = embeddingPlatform(usage)!;
                // prefer the concrete application id of the embedding source; fall back to
                // the generic platform name (WordPress / Moodle / ILIAS) when no appId is set
                const source = usage.appId || PLATFORM_NAMES[platform];
                return {
                    key: usageKey(usage),
                    iconUrl: this.nodeHelper.getSourceIconPath(platform),
                    icon: 'code',
                    label: source + ': ' + (usage.courseTitle || usage.courseId),
                    // show how many usages of the same course were clustered (>1 only)
                    badge: group.length > 1 ? group.length : undefined,
                };
            }),
        };
    });

    /** Compute the collection usages table. */
    private readonly collectionsTable = computed<UsageDetailTable | null>(() => {
        const usages = this.collectionUsages();
        if (!usages.length) {
            return null;
        }
        return {
            titleKey: 'WORKSPACE.METADATA.USAGES.COLLECTIONS_TITLE',
            columnKey: 'WORKSPACE.METADATA.USAGES.COLLECTIONS_COLUMN',
            emptyKey: 'WORKSPACE.METADATA.USAGES.NO_COLLECTIONS',
            rows: usages.map((usage, index) => ({
                key: 'collection-' + (usage.collection?.ref?.id ?? index),
                node: usage.collection,
                icon: 'layers',
                label: '',
                // absolute link to the collection detail page (…/components/collections?id=…)
                // leading "/" makes it absolute (routerLink is otherwise route-relative)
                link: ['/' + UIConstants.ROUTER_PREFIX + 'collections'],
                queryParams: { id: usage.collection?.ref?.id },
            })),
        };
    });

    /**
     * Detail tables for the currently selected metric (the embeddings view may show two
     * tables). Empty for a multi-selection or metrics without detail data.
     */
    protected readonly detailTables = computed<UsageDetailTable[]>(() => {
        // per-node detail lists cannot be aggregated over a multi-selection –
        // only the embeddings/usage count is shown, without detail tables
        if (this.isMultiSelection()) {
            return [];
        }
        if (this.selectedMetricId() === 'embedding') {
            return [this.embeddingsTable(), this.collectionsTable()].filter(
                (table): table is UsageDetailTable => table != null,
            );
        }
        // TODO: views / downloads / plays tables follow the same pattern once data exists
        return [];
    });

    /** Whether the selected metric has at least one detail table to show. */
    protected readonly hasDetail = computed(() => this.detailTables().length > 0);

    /**
     * The embeddings/usage metric is selected for a single node, but that node has no
     * usages at all – show an explanatory hint instead of an empty detail area.
     */
    protected readonly showNoUsagesHint = computed(
        () =>
            this.nodeIds().length === 1 &&
            this.showStatisticsPreview() &&
            this.isSelected('embedding') &&
            this.embeddingCount() === 0,
    );

    constructor() {
        // (re)load the action statistics whenever the selection or the timeframe changes
        effect(() => {
            const ids = this.nodeIds();
            const days = this.selectedRange();
            // skip loading for empty selections or folders/collections
            if (ids.length && this.showStatisticsPreview()) {
                this.loadStatistics(ids, days);
            } else {
                this.resetStatistics();
                this.lastStatsKey = null;
            }
        });

        // the usage lists are not timeframe-scoped – reload only on selection change.
        // loaded (and aggregated) for any selection so the embeddings/usage count stays
        // available; the per-node detail tables are hidden for a multi-selection.
        effect(() => {
            const ids = this.nodeIds();
            if (ids.length && this.showStatisticsPreview()) {
                this.loadEmbeddings(ids);
            } else {
                this.embeddings.set([]);
                this.collectionUsages.set([]);
            }
        });
    }

    /** Checks whether a given metric ID is selected. */
    protected isSelected(id: string): boolean {
        return this.selectedMetricId() === id;
    }

    /** Sets a given metric ID as the selected metric. */
    protected selectMetric(id: string): void {
        // only one card can be selected at a time
        this.selectedMetricId.set(id);
    }

    /** Sets a given days range as the selected timeframe range. */
    protected changeTimeframeRange(days: number): void {
        this.selectedRange.set(days);
    }

    /** Navigate to the admin statistics dashboard scoped to the current selection. */
    protected goToDashboard(): void {
        void this.router.navigate([UIConstants.ROUTER_PREFIX + 'admin'], {
            queryParams: { mode: 'STATISTICS', nodes: this.nodeIds().join(',') },
        });
    }

    /** Load view/download/play counts for the given nodes over the last `days` days. */
    private loadStatistics(ids: string[], days: number): void {
        const key = ids.join(',');
        // Only clear on a selection switch (so a different selection never shows the
        // previous one's numbers). A timeframe change on the same selection keeps the
        // current values visible until the new ones arrive, avoiding a visual flash to 0.
        if (this.lastStatsKey !== key) {
            this.resetStatistics();
            this.lastStatsKey = key;
        }
        // node statistics require the selective-statistics tool permission
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_SELECTIVE_STATISTICS_NODES)
            .subscribe((allowed) => {
                if (!this.isCurrentSelection(key)) {
                    return;
                }
                if (!allowed) {
                    this.resetStatistics();
                    return;
                }
                const dateTo = new Date();
                const dateFrom = new Date();
                dateFrom.setDate(dateFrom.getDate() - days);
                this.statisticApi
                    .getByNodes({
                        dateFrom: dateFrom.toISOString(),
                        dateTo: dateTo.toISOString(),
                        maxResults: 50000,
                        body: ids,
                    })
                    .subscribe({
                        next: (data) => {
                            if (!this.isCurrentSelection(key)) {
                                return;
                            }
                            const sum = (action: string): number =>
                                data.reduce((acc, entry) => acc + (entry.counts?.[action] ?? 0), 0);
                            const embeddedViews = sum(ACTION_VIEW_EMBEDDED);
                            // view count = direct material views + embedded views
                            this.viewCount.set(sum(ACTION_VIEW) + embeddedViews);
                            this.embeddedViewCount.set(embeddedViews);
                            this.downloadCount.set(sum(ACTION_DOWNLOAD));
                            this.playCount.set(sum(ACTION_PLAY));
                        },
                        error: () => this.resetStatistics(),
                    });
            });
    }

    /** Reset all statistics counters to zero. */
    private resetStatistics(): void {
        this.viewCount.set(0);
        this.embeddedViewCount.set(0);
        this.downloadCount.set(0);
        this.playCount.set(0);
    }

    /** Load the embeddings and collection usages of the given nodes. */
    private loadEmbeddings(ids: string[]): void {
        const key = ids.join(',');
        // clear stale lists while the reload is in flight (selection switch)
        this.embeddings.set([]);
        this.collectionUsages.set([]);
        forkJoin(ids.map((nodeId) => this.usageApi.getUsagesByNode({ nodeId }))).subscribe({
            next: (results) => {
                if (!this.isCurrentSelection(key)) {
                    return;
                }
                this.embeddings.set(results.flatMap((result) => result.usages ?? []));
            },
            error: () => this.embeddings.set([]),
        });
        forkJoin(
            ids.map((nodeId) => this.usageApi.getUsagesByNodeCollections({ nodeId })),
        ).subscribe({
            next: (results) => {
                if (!this.isCurrentSelection(key)) {
                    return;
                }
                // the endpoint's generated type is loose – the runtime items carry
                // the collection node under `collection`
                const entries = results.flat() as unknown as CollectionUsageEntry[];
                this.collectionUsages.set(
                    entries.filter((entry) => entry.collectionUsageType === 'ACTIVE'),
                );
            },
            error: () => this.collectionUsages.set([]),
        });
    }

    /** Guard against out-of-order responses when the selection changes quickly. */
    private isCurrentSelection(key: string): boolean {
        return this.selectionKey() === key;
    }
}

/** Stable key for a usage row. */
function usageKey(usage: Usage): string {
    return usage.guid ?? usage.nodeId;
}

/**
 * Group usages that refer to the same course – i.e. the same application (`appId`)
 * and course (`courseId`) – so they can be collapsed into a single row.
 * Insertion order is preserved.
 */
function clusterUsagesByCourse(usages: Usage[]): Usage[][] {
    const clusters = new Map<string, Usage[]>();
    for (const usage of usages) {
        // NUL separator avoids collisions between concatenated ids
        const key = (usage.appId ?? '') + '\u0000' + (usage.courseId ?? '');
        const group = clusters.get(key);
        if (group) {
            group.push(usage);
        } else {
            clusters.set(key, [usage]);
        }
    }
    return Array.from(clusters.values());
}

/**
 * Classify a usage into a supported embedding platform, or null if it is not a
 * relevant embedding (those are filtered out of the embeddings table):
 *  - CMS / wordpress → wordpress
 *  - LMS / moodle    → moodle
 *  - LMS / ilias     → ilias
 */
function embeddingPlatform(usage: Usage): EmbeddingPlatform | null {
    const type = (usage.appType || '').toUpperCase();
    const subtype = (usage.appSubtype || '').toLowerCase();
    if (type === 'CMS' && subtype === 'wordpress') {
        return 'wordpress';
    }
    if (type === 'LMS' && subtype === 'moodle') {
        return 'moodle';
    }
    if (type === 'LMS' && subtype === 'ilias') {
        return 'ilias';
    }
    return null;
}
