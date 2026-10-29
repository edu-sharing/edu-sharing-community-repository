import { TOPIC_PAGE_VIEW_TYPE } from '../../topic-page/shared/utils/collection-topic-page-util';
import { collectionSortIdOf, SORTS, SortId } from './collection-sorts';
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
    NodeRef,
    NodeService,
    ROOT,
} from 'ngx-edu-sharing-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { firstValueFrom, of } from 'rxjs';
import { AuthorityNamePipe, LocalEventsService } from 'ngx-edu-sharing-ui';
import { RestConnectorService, RestConstants, RestHelper } from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { SharedModule } from '../../../shared/shared.module';
import {
    CollectionDesignFormComponent,
    CollectionPreviewChange,
} from '../../collection-info/collection-design-form/collection-design-form.component';
import { CollectionEditorialGroupsFormComponent } from '../../collection-info/collection-editorial-groups-form/collection-editorial-groups-form.component';
import {
    ShareDialogData,
    ShareDialogResult,
} from '../../dialogs/dialog-modules/share-dialog/share-dialog-data';
import {
    ExtendedAcl,
    ShareDialogComponent,
} from '../../dialogs/dialog-modules/share-dialog/share-dialog.component';
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

/** the collection that is edited instead of a new one created */
export type EditCollectionConfig = {
    collectionId: string;
    /** edits only this step instead of passing all of them */
    step?: Step;
};

export type Step = 'DESIGN' | 'CONFIGURE' | 'PERMISSIONS';
/** who the collection can be given to after it was created */
type PermissionsKind = 'SHARE' | 'EDITORIAL_GROUPS';
/**
 * Creates or edits a collection in up to three steps: first its look (title, description, colour,
 * image), then its type, metadata and default sorting, and finally who it is given to. Nothing is
 * stored before the last step is confirmed.
 */
@Component({
    selector: 'es-create-collection-sidebar',
    templateUrl: 'create-collection-sidebar.component.html',
    styleUrls: ['create-collection-sidebar.component.scss'],
    imports: [
        SharedModule,
        MatStepperModule,
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
    private readonly connector = inject(RestConnectorService);
    private readonly createCollectionService = inject(CreateCollectionService);
    private readonly editorialSidebarService = inject(EditorialSidebarService);
    private readonly localEvents = inject(LocalEventsService);
    private readonly mdsService = inject(MdsService);
    private readonly mediacenterService = inject(MediacenterService);
    private readonly nodeApi = inject(NodeService);
    private readonly toast = inject(Toast);

    /** the collection the new one is created in (unset on the collections root) */
    readonly parent = input<Node>();
    readonly option = input<OptionState<EditCollectionConfig>>();

    protected readonly i18nPrefix = 'EDITORIAL.SIDEBAR.CREATE_COLLECTION.';
    protected readonly mds = viewChild<MdsEditorWrapperComponent>('mds');
    private readonly stepper = viewChild(MatStepper);
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
    /** whether the collection opens in its topic view, stored as the view type of the collection */
    protected readonly openAsTopicPage = signal(false);
    protected readonly saving = signal(false);
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
    /** the metadata the editor starts from */
    protected metadata: Values = {};
    /** the metadata values that are stored with the collection */
    private values: Values = {};
    /** what the share dialog handed over last, which is applied once the collection exists */
    private sharePermissions: ShareDialogResult | null = null;
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
    /** the steps in the order they are shown; access is only asked for if it can be given */
    protected readonly steps = computed<Step[]>(() => {
        if (this.singleStep()) {
            return [this.singleStep()];
        }
        return this.permissionsKind()
            ? ['DESIGN', 'CONFIGURE', 'PERMISSIONS']
            : ['DESIGN', 'CONFIGURE'];
    });
    protected readonly isFirstStep = computed(() => this.step() === this.steps()[0]);
    protected readonly isLastStep = computed(() => this.step() === this.steps().at(-1));
    /** how access is given after the creation; none if the user cannot invite or the type has no such step */
    protected readonly permissionsKind = computed<PermissionsKind | null>(() => {
        if (!this.canInvite() || !this.canChangePermissions() || this.type() === 'MEDIA_CENTER') {
            return null;
        }
        return this.type() === 'EDITORIAL' && this.invitationType() === 'EditorialGroups'
            ? 'EDITORIAL_GROUPS'
            : 'SHARE';
    });
    /** the id of the collection that is edited, unset when a new one is created */
    protected readonly editId = computed(
        () => (this.option()?.optionConfig as EditCollectionConfig)?.collectionId ?? null,
    );
    protected readonly isEdit = computed(() => !!this.editId());
    /** the only step that is edited, unset if all steps are passed */
    private readonly singleStep = computed(
        () => (this.option()?.optionConfig as EditCollectionConfig)?.step ?? null,
    );

    /** the collections from the top level down to the one the new collection is created in */
    private readonly parentPath = rxResource({
        params: () => {
            const parent = this.parent();
            return parent && parent.ref.id !== ROOT && !this.isEdit() ? parent.ref.id : null;
        },
        stream: ({ params }) =>
            params ? this.nodeApi.getParents(params, { fullPath: false }) : of(null),
    });
    /** `null` when the location is not shown (editing keeps the collection where it is) */
    protected readonly location = computed<string[] | null>(() => {
        if (this.isEdit()) {
            return null;
        }
        const parent = this.parent();
        if (!parent || parent.ref.id === ROOT) {
            return [];
        }
        // until the path has arrived, the collection itself is the best known location
        const path = this.parentPath.hasValue() ? this.parentPath.value()?.nodes : null;
        const nodes = path?.length ? [...path].reverse() : [parent];
        return nodes.map((node) => node.title || node.name);
    });

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

    /** Follows a click on a step of the stepper, and shows the step that is current if it is blocked. */
    protected async onStepperChange(index: number): Promise<void> {
        if (!this.saving()) {
            await this.goToStep(this.steps()[index]);
        }
        const stepper = this.stepper();
        if (stepper) {
            stepper.selectedIndex = this.steps().indexOf(this.step());
        }
    }

    /** Leaves the form the same way the back arrow of the sidebar header does. */
    protected cancel(): void {
        void this.editorialSidebarService.editorialSidebar?.goBack();
    }

    protected async previousStep(): Promise<void> {
        if (this.step() === 'PERMISSIONS') {
            await this.rememberPermissions();
            this.metadata = this.values;
            this.step.set('CONFIGURE');
        } else if (this.step() === 'CONFIGURE') {
            this.step.set('DESIGN');
        }
    }

    /** Moves to another step, passing the steps in between so each of them keeps what was entered. */
    protected async goToStep(target: Step): Promise<void> {
        const order = this.steps();
        while (this.step() !== target) {
            const before = this.step();
            if (order.indexOf(target) > order.indexOf(before)) {
                await this.next();
            } else {
                await this.previousStep();
            }
            if (this.step() === before) {
                // blocked, e.g. by a missing title
                return;
            }
        }
    }

    protected async next(): Promise<void> {
        if (this.step() === 'DESIGN') {
            if (!this.hasTitle()) {
                this.toast.error(null, 'COLLECTIONS.ENTER_NAME');
                return;
            }
            this.step.set('CONFIGURE');
            return;
        }
        const values = this.hasMetadata() ? await this.mds()?.getValues() : {};
        if (values === null) {
            // a required metadata value is missing; the editor shows which one
            return;
        }
        this.values = values ?? {};
        this.metadata = this.values;
        this.preparePermissions();
        this.step.set('PERMISSIONS');
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
        this.values = {};
        setTimeout(() => this.mds()?.loadMds(true));
    }

    /** stores the collection together with who it is given to */
    protected async save(): Promise<void> {
        if (this.step() === 'DESIGN' && !this.hasTitle()) {
            this.toast.error(null, 'COLLECTIONS.ENTER_NAME');
            return;
        }
        if (this.step() === 'CONFIGURE') {
            const values = this.hasMetadata() ? await this.mds()?.getValues() : {};
            if (values === null) {
                // a required metadata value is missing; the editor shows which one
                return;
            }
            this.values = values ?? {};
        } else if (this.step() === 'PERMISSIONS' && !(await this.rememberPermissions(true))) {
            return;
        }
        this.saving.set(true);
        this.editorialSidebarService.sidebarLoading.set(true);
        try {
            if (this.isEdit()) {
                await this.update(this.values);
                if (!(await this.applyPermissionsToEdited())) {
                    return;
                }
            } else {
                const created = await this.createNew(this.values);
                try {
                    await this.applyPermissionsToCreated(created);
                } catch (error) {
                    // the collection exists by now, so a retry would only run into its name
                    this.toast.error(error);
                }
                // announced first, so the list that reloads because of the refresh knows what to select
                this.localEvents.nodesCreated.emit([created]);
                this.refreshParent();
            }
            this.editorialSidebarService.close();
        } catch (error) {
            this.handleError(error);
        } finally {
            this.saving.set(false);
            this.editorialSidebarService.sidebarLoading.set(false);
        }
    }

    /** stores the chosen default view in the collection data that is sent */
    private applyViewType(): void {
        this.draft.collection.viewtype = this.openAsTopicPage() ? TOPIC_PAGE_VIEW_TYPE : undefined;
    }

    private async createNew(values: Values): Promise<Node> {
        this.applyViewType();
        const created = await this.createCollectionService.create({
            parent: this.parent() ?? null,
            draft: this.draft,
            type: this.type(),
            metadata: this.getMetadata(values),
            subcollectionSort: SORTS.subcollections[this.subcollectionSort()],
            referenceSort: SORTS.references[this.referenceSort()],
            image: this.image,
        });
        this.toast.toast(this.i18nPrefix + 'CREATED');
        return created;
    }

    private async update(values: Values): Promise<Node> {
        this.applyViewType();
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

    /** shows the access of the collection; a collection that does not exist yet is stood in for */
    private preparePermissions(): void {
        if (this.permissionsKind() !== 'SHARE') {
            return;
        }
        const node = this.isEdit() ? this.draft : this.getPlaceholderNode();
        this.shareConfig = {
            nodes: [node],
            sendMessages: true,
            sendToApi: this.isEdit(),
            currentPermissions: this.isEdit()
                ? null
                : this.sharePermissions?.permissions ??
                  ({ inherited: false, permissions: [] } as ExtendedAcl),
        };
    }

    /** the share dialog needs a node to show, while the collection is only stored at the end */
    private getPlaceholderNode(): Node {
        return {
            title: this.draft.title,
            ref: {} as NodeRef,
            icon: { url: this.connector.getThemeMimeIconSvg('collection.svg') },
            aspects: [RestConstants.CCM_ASPECT_COLLECTION],
            properties: {},
            isDirectory: true,
        } as Node;
    }

    protected onSharePermissionsChange(result: ShareDialogResult): void {
        this.sharePermissions = result;
    }

    /**
     * Takes over what the user chose in the access step, so it survives a step back.
     * @returns whether the choice is valid
     */
    private async rememberPermissions(validate = false): Promise<boolean> {
        if (this.permissionsKind() === 'EDITORIAL_GROUPS') {
            const acl = this.editorialGroupsForm()?.getAcl();
            if (acl) {
                this.editorialInitialPermissions.set(acl.permissions as Ace[]);
                this.editorialPublic.set(
                    acl.permissions.some(
                        (ace) => ace.authority.authorityName === RestConstants.AUTHORITY_EVERYONE,
                    ),
                );
            }
            return true;
        }
        if (this.isEdit()) {
            // the share dialog of an existing collection stores its choice itself
            return true;
        }
        // the dialog only reports back if its choice is valid
        this.sharePermissions = null;
        await this.shareDialog()?.save();
        return !!this.sharePermissions || !validate;
    }

    private async applyPermissionsToCreated(created: Node): Promise<void> {
        if (this.permissionsKind() === 'EDITORIAL_GROUPS') {
            await this.saveEditorialGroups(created.ref.id);
            return;
        }
        const acl = this.sharePermissions?.permissions;
        if (this.permissionsKind() !== 'SHARE' || !acl?.permissions?.length) {
            return;
        }
        await firstValueFrom(
            this.nodeApi.setPermissions(
                created.ref.id,
                RestHelper.copyAndCleanPermissions(acl.permissions as Ace[], false),
                {
                    sendMail: this.sharePermissions.notify,
                    mailText: this.sharePermissions.notifyMessage,
                    sendCopy: false,
                },
            ),
        );
    }

    /** @returns whether the access was stored, which is also the case if there is none to store */
    private async applyPermissionsToEdited(): Promise<boolean> {
        if (this.step() !== 'PERMISSIONS') {
            return true;
        }
        let saved = true;
        if (this.permissionsKind() === 'EDITORIAL_GROUPS') {
            await this.saveEditorialGroups(this.draft.ref.id);
        } else {
            saved = await this.saveShare(this.draft);
        }
        this.refreshEdited();
        return saved;
    }

    private async saveEditorialGroups(id: string): Promise<void> {
        const acl = this.editorialGroupsForm()?.getAcl();
        await firstValueFrom(
            this.nodeApi.setPermissions(
                id,
                RestHelper.copyAndCleanPermissions(acl.permissions, false),
                { sendMail: false, sendCopy: false },
            ),
        );
    }

    /** the share dialog stores the permissions itself and announces the node it stored them for */
    private async saveShare(node: Node): Promise<boolean> {
        let saved = false;
        const subscription = this.localEvents.nodesChanged.subscribe((nodes) => {
            saved ||= nodes.some((changed) => changed?.ref?.id === node.ref.id);
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
        switch (this.singleStep()) {
            case 'DESIGN':
                return 'EDITORIAL.OPTIONS.COLLECTION_EDIT_DESIGN';
            case 'CONFIGURE':
                return 'EDITORIAL.OPTIONS.COLLECTION_EDIT_CONFIGURE';
            case 'PERMISSIONS':
                return 'EDITORIAL.OPTIONS.COLLECTION_SHARE';
        }
        return this.i18nPrefix + (this.isEdit() ? 'EDIT_TITLE' : 'TITLE');
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
            this.step.set(this.singleStep() ?? 'DESIGN');
            if (this.step() === 'PERMISSIONS') {
                this.preparePermissions();
            }
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
        this.openAsTopicPage.set(node.collection.viewtype === TOPIC_PAGE_VIEW_TYPE);
        this.initialSorts = {
            subcollections: collectionSortIdOf(
                'subcollections',
                node.properties?.[RestConstants.CCM_PROP_COLLECTION_SUBCOLLECTION_ORDER_MODE],
            ),
            references: collectionSortIdOf(
                'references',
                node.properties?.[RestConstants.CCM_PROP_COLLECTION_ORDER_MODE],
            ),
        };
        this.subcollectionSort.set(this.initialSorts.subcollections);
        this.referenceSort.set(this.initialSorts.references);
        this.canChangePermissions.set(
            RestHelper.hasAccessPermission(node, RestConstants.ACCESS_CHANGE_PERMISSIONS),
        );
        if (this.permissionsKind() === 'EDITORIAL_GROUPS') {
            const permissions = await firstValueFrom(this.nodeApi.getPermissions(node.ref.id));
            const local = (permissions.localPermissions?.permissions ?? []) as Ace[];
            this.editorialInitialPermissions.set(local);
            this.editorialPublic.set(
                local.some(
                    (ace) => ace.authority.authorityName === RestConstants.AUTHORITY_EVERYONE,
                ),
            );
        }
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
