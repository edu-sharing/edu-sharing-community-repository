import {
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    input,
    signal,
    viewChild,
} from '@angular/core';
import {
    Ace,
    AuthenticationService,
    ConfigService,
    CollectionsTypeConfig,
    MdsService,
    MediacenterService,
    Node,
    NodeService,
    ROOT,
} from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { AuthorityNamePipe, LocalEventsService } from 'ngx-edu-sharing-ui';
import { RestConstants, RestHelper } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { SharedModule } from '../../../shared/shared.module';
import {
    CollectionDesignFormComponent,
    CollectionPreviewChange,
} from '../../collection-info/collection-design-form/collection-design-form.component';
import { CollectionEditorialGroupsFormComponent } from '../../collection-info/collection-editorial-groups-form/collection-editorial-groups-form.component';
import { ShareDialogData } from '../../dialogs/dialog-modules/share-dialog/share-dialog-data';
import { ShareDialogComponent } from '../../dialogs/dialog-modules/share-dialog/share-dialog.component';
import { ShareDialogModule } from '../../dialogs/dialog-modules/share-dialog/share-dialog.module';
import { MdsEditorWrapperComponent } from '../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsModule } from '../../mds/mds.module';
import { Values } from '../../mds/types/types';
import { OptionState } from '../editorial-sidebar.component';
import { EditorialSidebarService } from '../editorial-sidebar.service';
import { SidebarStickyFooterComponent } from '../sidebar-sticky-footer/sidebar-sticky-footer.component';
import {
    CreateCollectionService,
    NewCollectionSort,
    NewCollectionType,
} from './create-collection.service';

/** where the creation was started from, which decides what the first "back" does */
export type CreateCollectionConfig = {
    /** `add` comes from the "add collection" choice, `menu` straight from the create menu */
    entry: 'add' | 'menu';
};

/** the collection that is edited instead of a new one created */
export type EditCollectionConfig = {
    collectionId: string;
};

type Step = 'DESIGN' | 'CONFIGURE' | 'PERMISSIONS';
/** who the collection can be given to after it was created */
type PermissionsKind = 'SHARE' | 'EDITORIAL_GROUPS';
type SortId = 'TITLE' | 'MODIFIED' | 'CREATED';

/** the ways children can be sorted by default, with the property each one stands for */
const SORTS: { [kind in 'subcollections' | 'references']: { [id in SortId]?: NewCollectionSort } } =
    {
        subcollections: {
            TITLE: { property: RestConstants.CM_PROP_TITLE, ascending: true },
            MODIFIED: { property: RestConstants.CM_MODIFIED_DATE, ascending: false },
            CREATED: { property: RestConstants.CM_PROP_C_CREATED, ascending: false },
        },
        references: {
            TITLE: { property: RestConstants.LOM_PROP_TITLE, ascending: true },
            MODIFIED: { property: RestConstants.CM_MODIFIED_DATE, ascending: false },
            CREATED: { property: RestConstants.CM_PROP_C_CREATED, ascending: false },
        },
    };

/**
 * Creates or edits a collection in two steps: first its look (title, description, colour, image),
 * then its type, metadata and default sorting. Once it is stored, a third step gives others access
 * to it.
 */
@Component({
    selector: 'es-create-collection-sidebar',
    templateUrl: 'create-collection-sidebar.component.html',
    styleUrls: ['create-collection-sidebar.component.scss'],
    imports: [
        SharedModule,
        MdsModule,
        ShareDialogModule,
        CollectionDesignFormComponent,
        CollectionEditorialGroupsFormComponent,
        SidebarStickyFooterComponent,
    ],
})
export class CreateCollectionSidebarComponent {
    private readonly authenticationService = inject(AuthenticationService);
    private readonly authorityNamePipe = inject(AuthorityNamePipe);
    private readonly configService = inject(ConfigService);
    private readonly createCollectionService = inject(CreateCollectionService);
    private readonly editorialSidebarService = inject(EditorialSidebarService);
    private readonly localEvents = inject(LocalEventsService);
    private readonly mdsService = inject(MdsService);
    private readonly mediacenterService = inject(MediacenterService);
    private readonly nodeApi = inject(NodeService);
    private readonly toast = inject(Toast);

    /** the collection the new one is created in (unset on the collections root) */
    readonly parent = input<Node>();
    readonly option = input<OptionState<CreateCollectionConfig | EditCollectionConfig>>();

    protected readonly i18nPrefix = 'EDITORIAL.SIDEBAR.CREATE_COLLECTION.';
    protected readonly mds = viewChild<MdsEditorWrapperComponent>('mds');
    private readonly shareDialog = viewChild(ShareDialogComponent);
    private readonly editorialGroupsForm = viewChild(CollectionEditorialGroupsFormComponent);
    protected readonly step = signal<Step>('DESIGN');
    protected readonly colors = signal<string[] | null>(null);
    protected readonly type = signal<NewCollectionType>('default');
    protected readonly availableTypes = signal<NewCollectionType[]>(['default']);
    /** the sorting options by id, in the order they are offered */
    protected readonly sortIds = Object.keys(SORTS.subcollections) as SortId[];
    protected readonly subcollectionSort = signal<SortId | null>('TITLE');
    protected readonly referenceSort = signal<SortId | null>('TITLE');
    /** whether the collection opens as a topic page; no collection property backs it yet */
    protected readonly openAsTopicPage = signal(false);
    protected readonly saving = signal(false);
    /** the collection once it was created, which the access is given to */
    private readonly created = signal<Node | null>(null);
    private readonly canInvite = signal(false);
    protected readonly shareToAll = signal(false);
    /** whether the user may change who the collection is given to */
    private readonly canChangePermissions = signal(true);
    /** what the collection was given to before, which the editorial groups start from */
    protected readonly editorialInitialPermissions = signal<Ace[]>([]);
    protected readonly editorialPublic = signal(true);
    private readonly invitationType =
        signal<CollectionsTypeConfig['invitationType']>('EditorialGroups');
    protected shareConfig: ShareDialogData | null = null;
    /** the type whose description is currently shown */
    protected readonly infoType = signal<NewCollectionType | null>(null);
    /** the keys of the descriptions that explain a type */
    protected readonly typeInfoKeys: { [type in NewCollectionType]: string[] } = {
        default: [
            'COLLECTIONS.CREATE.PRIVATE',
            'COLLECTIONS.CREATE.SHARED',
            'COLLECTIONS.CREATE.PUBLIC',
        ],
        EDITORIAL: ['COLLECTIONS.CREATE.EDITORIAL'],
        MEDIA_CENTER: ['COLLECTIONS.CREATE.MEDIA_CENTER'],
    };
    /** the ids of the metadata groups the metadata set offers */
    private readonly mdsGroupIds = signal<string[]>([]);
    protected metadata: Values = {};
    /** the collection as it is edited; the design form writes into it */
    protected draft: Node;

    private image: File | null = null;
    private removeImage = false;
    /** the sorting the edited collection had, to leave it as it is unless it was changed */
    private initialSorts: { subcollections: SortId | null; references: SortId | null } = {
        subcollections: null,
        references: null,
    };
    private mediacenterName: string | null = null;
    private editorialConfig: CollectionsTypeConfig = {
        invitationType: 'EditorialGroups',
        metadataGroup: 'collection_editorial',
    };

    protected readonly metadataGroupId = computed(() => {
        switch (this.type()) {
            case 'MEDIA_CENTER':
                return 'collection_media_center';
            case 'EDITORIAL':
            default:
                // types without a group of their own share the editorial one
                return this.editorialConfig.metadataGroup;
        }
    });
    /** a type without a group, or a set that lacks it, has no metadata to ask for */
    protected readonly hasMetadata = computed(() =>
        this.mdsGroupIds().includes(this.metadataGroupId()),
    );
    protected readonly isDesignStep = computed(() => this.step() === 'DESIGN');
    /** how access is given after the creation; none if the user cannot invite or the type has no such step */
    protected readonly permissionsKind = computed<PermissionsKind | null>(() => {
        if (!this.canInvite() || !this.canChangePermissions() || this.type() === 'MEDIA_CENTER') {
            return null;
        }
        return this.type() === 'EDITORIAL' && this.invitationType() === 'EditorialGroups'
            ? 'EDITORIAL_GROUPS'
            : 'SHARE';
    });
    protected readonly entry = computed(
        () => (this.option()?.optionConfig as CreateCollectionConfig)?.entry ?? 'add',
    );
    /** the id of the collection that is edited, unset when a new one is created */
    protected readonly editId = computed(
        () => (this.option()?.optionConfig as EditCollectionConfig)?.collectionId ?? null,
    );
    protected readonly isEdit = computed(() => !!this.editId());

    constructor() {
        void this.init();
        effect(() => {
            this.editorialSidebarService.titleOverride.set(this.getTitle());
        });
        inject(DestroyRef).onDestroy(() => this.editorialSidebarService.titleOverride.set(null));
    }

    protected onPreviewChange(change: CollectionPreviewChange): void {
        this.image = change.file;
        this.removeImage = change.delete;
    }

    protected hasTitle(): boolean {
        return !!this.draft?.title?.trim();
    }

    protected back(): void {
        if (this.step() === 'CONFIGURE') {
            this.step.set('DESIGN');
        } else if (this.entry() === 'add' && !this.isEdit()) {
            this.editorialSidebarService.showOption({ option: 'ADD_COLLECTION', trap: false });
        } else {
            this.editorialSidebarService.close();
        }
    }

    protected next(): void {
        if (!this.hasTitle()) {
            this.toast.error(null, 'COLLECTIONS.ENTER_NAME');
            return;
        }
        this.step.set('CONFIGURE');
    }

    protected toggleInfo(type: NewCollectionType): void {
        this.infoType.update((current) => (current === type ? null : type));
    }

    protected setType(type: NewCollectionType): void {
        this.type.set(type);
        this.draft.collection.authorFreetext =
            type === 'MEDIA_CENTER' ? this.mediacenterName : null;
        // the metadata of the previous type is not part of the new one
        this.metadata = {};
        setTimeout(() => this.mds()?.loadMds(true));
    }

    protected async create(): Promise<void> {
        const values = this.hasMetadata() ? await this.mds()?.getValues() : {};
        if (values === null) {
            // a required metadata value is missing; the editor shows which one
            return;
        }
        this.saving.set(true);
        this.editorialSidebarService.sidebarLoading.set(true);
        try {
            const stored = this.isEdit() ? await this.update(values) : await this.createNew(values);
            if (this.permissionsKind()) {
                await this.showPermissions(stored);
            } else {
                this.editorialSidebarService.close();
            }
        } catch (error) {
            this.handleError(error);
        } finally {
            this.saving.set(false);
            this.editorialSidebarService.sidebarLoading.set(false);
        }
    }

    private async createNew(values: Values): Promise<Node> {
        const created = await this.createCollectionService.create({
            parent: this.parent() ?? null,
            draft: this.draft,
            type: this.type(),
            metadata: this.getMetadata(values),
            subcollectionSort: SORTS.subcollections[this.subcollectionSort()],
            referenceSort: SORTS.references[this.referenceSort()],
            image: this.image,
        });
        this.refreshParent();
        this.toast.toast(this.i18nPrefix + 'CREATED');
        return created;
    }

    private async update(values: Values): Promise<Node> {
        await this.createCollectionService.update({
            collection: this.draft,
            metadata: this.getMetadata(values),
            subcollectionSort: this.getChangedSort('subcollections', this.subcollectionSort()),
            referenceSort: this.getChangedSort('references', this.referenceSort()),
            image: this.image,
            removeImage: this.removeImage && !this.image,
        });
        this.refreshEdited();
        this.toast.toast(this.i18nPrefix + 'SAVED');
        return this.draft;
    }

    private getMetadata(values: Values): Values {
        return Object.fromEntries(
            Object.entries(values ?? {}).filter(([, value]) => value?.length),
        );
    }

    private getChangedSort(
        kind: 'subcollections' | 'references',
        selected: SortId | null,
    ): NewCollectionSort | null {
        return selected && selected !== this.initialSorts[kind] ? SORTS[kind][selected] : null;
    }

    /** saves who the collection is given to and ends the creation */
    protected async finish(): Promise<void> {
        const created = this.created();
        this.saving.set(true);
        this.editorialSidebarService.sidebarLoading.set(true);
        try {
            const saved =
                this.permissionsKind() === 'EDITORIAL_GROUPS'
                    ? await this.saveEditorialGroups(created)
                    : await this.saveShare(created);
            if (saved) {
                this.refreshParent();
                this.editorialSidebarService.close();
            }
        } catch (error) {
            this.toast.error(error);
        } finally {
            this.saving.set(false);
            this.editorialSidebarService.sidebarLoading.set(false);
        }
    }

    private async showPermissions(created: Node): Promise<void> {
        // the share dialog needs everything the node carries, e.g. its aspects and access
        const node = await firstValueFrom(this.nodeApi.getNode(created.ref.id));
        this.created.set(node);
        if (this.permissionsKind() === 'EDITORIAL_GROUPS' && this.isEdit()) {
            const permissions = await firstValueFrom(this.nodeApi.getPermissions(node.ref.id));
            const local = (permissions.localPermissions?.permissions ?? []) as Ace[];
            this.editorialInitialPermissions.set(local);
            this.editorialPublic.set(
                local.some(
                    (ace) => ace.authority.authorityName === RestConstants.AUTHORITY_EVERYONE,
                ),
            );
        }
        this.shareConfig = { nodes: [node], sendMessages: true, sendToApi: true };
        this.step.set('PERMISSIONS');
    }

    private async saveEditorialGroups(created: Node): Promise<boolean> {
        const acl = this.editorialGroupsForm().getAcl();
        await firstValueFrom(
            this.nodeApi.setPermissions(
                created.ref.id,
                RestHelper.copyAndCleanPermissions(acl.permissions, false),
                { sendMail: false, sendCopy: false },
            ),
        );
        this.toast.toast('WORKSPACE.PERMISSIONS_UPDATED');
        return true;
    }

    /** the share dialog stores the permissions itself and announces the node it stored them for */
    private async saveShare(created: Node): Promise<boolean> {
        let saved = false;
        const subscription = this.localEvents.nodesChanged.subscribe((nodes) => {
            saved ||= nodes.some((node) => node?.ref?.id === created.ref.id);
        });
        try {
            await this.shareDialog().save();
        } finally {
            subscription.unsubscribe();
        }
        return saved;
    }

    /**
     * Lets the list the collection was created in show it. The top level has no node of its own: the
     * page stands in for it with a collection that has no repository, which listeners would try to
     * load children of, so only its id is announced.
     */
    private refreshParent(): void {
        const parent = this.parent();
        this.localEvents.nodesChanged.emit([
            parent && parent.ref.id !== ROOT ? parent : ({ ref: { id: ROOT } } as Node),
        ]);
    }

    /** lets the lists that show the collection, and the one it is listed in, pick up the change */
    private refreshEdited(): void {
        const parent = this.parent();
        const nodes = [this.draft];
        if (parent && parent.ref.id !== ROOT && parent.ref.id !== this.draft.ref.id) {
            nodes.push(parent);
        }
        this.localEvents.nodesChanged.emit(nodes);
    }

    private getTitle(): string {
        switch (this.step()) {
            case 'DESIGN':
                return this.i18nPrefix + 'DESIGN_TITLE';
            case 'CONFIGURE':
                return this.i18nPrefix + 'CONFIGURE_TITLE';
            default:
                return this.permissionsKind() === 'EDITORIAL_GROUPS'
                    ? 'COLLECTIONS.STEP.EDITORIAL_GROUPS'
                    : 'COLLECTIONS.EDIT_PERMISSIONS';
        }
    }

    private handleError(error: any): void {
        if (error?.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.step.set('DESIGN');
            this.toast.error(null, 'COLLECTIONS.TOAST.DUPLICATE_NAME');
        } else if (JSON.stringify(error?.error ?? '').includes('Invalid property value')) {
            this.step.set('DESIGN');
            this.toast.error(null, 'COLLECTIONS.TOAST.INVALID_NAME');
        } else {
            this.toast.error(error);
        }
    }

    private async init(): Promise<void> {
        const colors = await this.configService.get<string[]>(
            'collections.colors',
            RestConstants.DEFAULT_COLLECTION_COLORS,
        );
        this.editorialConfig = await this.configService.get<CollectionsTypeConfig>(
            'collections.types.editorial',
            this.editorialConfig,
        );
        this.invitationType.set(this.editorialConfig.invitationType);
        const set = await firstValueFrom(this.mdsService.getMetadataSet({}));
        this.mdsGroupIds.set((set.groups ?? []).map((group) => group.id));
        const permissions = await this.loadToolpermissions();
        if (this.isEdit()) {
            await this.loadEdited();
        } else {
            this.draft = {
                title: '',
                collection: { description: '', color: colors[0] },
            } as Node;
            await this.loadAvailableTypes(permissions);
        }
        this.colors.set(colors);
    }

    /** takes over what the collection that is edited has now */
    private async loadEdited(): Promise<void> {
        const node = await firstValueFrom(this.nodeApi.getNode(this.editId()));
        this.draft = node;
        this.type.set(
            node.collection.type === RestConstants.COLLECTIONTYPE_EDITORIAL
                ? 'EDITORIAL'
                : node.collection.type === RestConstants.COLLECTIONTYPE_MEDIA_CENTER
                ? 'MEDIA_CENTER'
                : 'default',
        );
        this.metadata = node.properties as Values;
        this.initialSorts = {
            subcollections: this.getSortId(
                'subcollections',
                node.properties?.[RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE],
            ),
            references: this.getSortId(
                'references',
                node.properties?.[RestConstants.CCM_PROP_COLLECTION_ORDER_MODE],
            ),
        };
        this.subcollectionSort.set(this.initialSorts.subcollections);
        this.referenceSort.set(this.initialSorts.references);
        this.canChangePermissions.set(
            RestHelper.hasAccessPermission(node, RestConstants.ACCESS_CHANGE_PERMISSIONS),
        );
    }

    /** the sorting a collection stores as property; a collection that has none is sorted by change */
    private getSortId(kind: 'subcollections' | 'references', stored?: string[]): SortId | null {
        if (!stored?.length) {
            return 'MODIFIED';
        }
        const id = this.sortIds.find((sortId) => SORTS[kind][sortId].property === stored[0]);
        return id ?? null;
    }

    private async loadToolpermissions(): Promise<{
        editorial: boolean;
        everyone: boolean;
        invite: boolean;
    }> {
        const [editorial, everyone, invite] = await Promise.all([
            this.authenticationService.hasToolpermission(
                RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
            ),
            this.authenticationService.hasToolpermission(
                RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
            ),
            this.authenticationService.hasToolpermission(RestConstants.TOOLPERMISSION_INVITE),
        ]);
        this.canInvite.set(invite);
        this.shareToAll.set(everyone);
        return { editorial, everyone, invite };
    }

    /** the types the user may create, which a collection below an editorial one cannot leave */
    private async loadAvailableTypes(permissions: {
        editorial: boolean;
        everyone: boolean;
        invite: boolean;
    }): Promise<void> {
        const { editorial, everyone, invite } = permissions;
        if (this.parent()?.collection?.type === RestConstants.COLLECTIONTYPE_EDITORIAL) {
            this.availableTypes.set(['EDITORIAL']);
            this.type.set('EDITORIAL');
            return;
        }
        const types: NewCollectionType[] = ['default'];
        if (editorial && everyone) {
            types.push('EDITORIAL');
        }
        const mediacenters = (
            await this.mediacenterService.getMediacenters({ repository: '-home-' }).toPromise()
        ).filter((mediacenter) => mediacenter.administrationAccess);
        if (invite && mediacenters.length === 1) {
            types.push('MEDIA_CENTER');
            this.mediacenterName = this.authorityNamePipe.transform(mediacenters[0] as any, null);
        }
        this.availableTypes.set(types);
    }
}
