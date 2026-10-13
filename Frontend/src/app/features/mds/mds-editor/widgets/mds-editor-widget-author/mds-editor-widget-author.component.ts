import {
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    inject,
    Input,
    OnInit,
    signal,
    ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatTabGroup } from '@angular/material/tabs';
import { BehaviorSubject } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { MdsWidget, Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestIamService } from '../../../../../core-module/rest/services/rest-iam.service';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { VCard } from 'ngx-edu-sharing-ui';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { Attributes } from '../../util/parse-attributes';
import { authorIsEmpty } from '../../util/native-widget-completion';
import { MainNavService } from '../../../../../main/navigation/main-nav.service';
import { DialogsService } from '../../../../dialogs/dialogs.service';
import { BulkMode, Constraints, InputStatus, Values } from '../../../types/types';
import { applyAuthorValues } from '../../util/author-bulk-values';

export interface AuthorData {
    freetext: string;
    author: VCard;
}
enum DefaultTab {
    freetext = 'freetext',
    vcard = 'vcard',
}
@Component({
    selector: 'es-mds-editor-widget-author',
    templateUrl: './mds-editor-widget-author.component.html',
    styleUrls: ['./mds-editor-widget-author.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetAuthorComponent implements OnInit, NativeWidgetComponent {
    mdsEditorValues = inject(MdsEditorInstanceService);
    private iamApi = inject(RestIamService);
    private mainNavService = inject(MainNavService);
    ui = inject(UIService);
    private dialogs = inject(DialogsService);
    private destroyRef = inject(DestroyRef);
    private changeDetectorRef = inject(ChangeDetectorRef);

    static readonly constraints: Constraints = {
        requiresNode: false,
        supportsBulk: true,
        // the widget only provides tabs with input fields, there is no read-only representation
        supportsViewer: false,
    };
    attributes: Attributes;
    /** carries the bulk mode; not set when embedded, e.g. in the license dialog */
    @Input() widget: Widget;
    @ViewChild(MatTabGroup) tabGroup: MatTabGroup;
    @Input() showContributorDialog = true;
    _nodes: Node[];
    hasChanges = new BehaviorSubject<boolean>(false);
    status = new BehaviorSubject<InputStatus>('VALID');
    /** whether the widget currently holds no author value (used for the completion status) */
    isEmpty = new BehaviorSubject<boolean>(true);
    /**
     * 'mandatory' / 'mandatoryForPublish' if the widget is configured as required via the
     * `required` attribute in the mds template, e.g. `<author required="mandatory">`
     */
    isRequired: MdsWidget['isRequired'] | null = null;
    /**
     * whether the missing-required hint should be displayed (after a save attempt or interaction)
     */
    showMissingRequiredHint = false;
    /**
     * minimal "fake" widget passed to the container so it renders the required marker (`*`) from
     * `definition.isRequired`, reusing the existing marker logic. `null` when not required.
     */
    private _requiredWidget: Partial<Widget> | null = null;
    authorTab = 0;
    author: AuthorData;
    /**
     * is the current editing user the real author (matched by id)
     */
    userAuthor: boolean;
    private initialAuthor: AuthorData;
    /** whether the nodes differ in the field */
    mixedFreetext = false;
    mixedAuthor = false;
    /** an emptied field with differing values looks unchanged, so edits are tracked separately */
    private freetextTouched = false;
    private authorTouched = false;
    private readonly bulkMode = signal<BulkMode>(null);
    /** locked while the bulk switch is off */
    readonly fieldsDisabled = computed(
        () => this.isBulk && !!this.widget && this.bulkMode() === 'no-change',
    );

    ngOnInit(): void {
        this.widget
            ?.observeBulkMode()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((mode) => {
                this.bulkMode.set(mode);
                if (this.initialAuthor) {
                    this.onChange();
                }
            });
        this.mdsEditorValues.nodes$.pipe(filter((n) => n != null)).subscribe((nodes) => {
            void this.updateValues(nodes);
        });
        this.mdsEditorValues.values$.pipe(filter((v) => v != null)).subscribe((values) => {
            void this.updateValues([{ properties: values }] as Node[]);
        });
    }
    get isBulk(): boolean {
        return !!this.mdsEditorValues.editorBulkMode?.isBulk;
    }

    /** whether the input is applied when saving; always true without a bulk switch */
    private get isEditingActive(): boolean {
        return !this.isBulk || !this.widget || this.bulkMode() !== 'no-change';
    }

    private get freetextChanged(): boolean {
        return this.mixedFreetext
            ? this.freetextTouched
            : this.initialAuthor.freetext !== this.author.freetext;
    }

    private get authorChanged(): boolean {
        return this.mixedAuthor
            ? this.authorTouched
            : this.initialAuthor.author.getDisplayName() !== this.author.author.getDisplayName();
    }

    showMixed(field: 'freetext' | 'author'): boolean {
        return field === 'freetext'
            ? this.mixedFreetext && !this.freetextTouched
            : this.mixedAuthor && !this.authorTouched;
    }

    onFreetextChange(): void {
        this.freetextTouched = true;
        this.onChange();
    }

    onAuthorChange(): void {
        this.authorTouched = true;
        this.onChange();
    }

    onChange(): void {
        this.hasChanges.next(this.isEditingActive && (this.freetextChanged || this.authorChanged));
        this.updateStatus();
    }

    /**
     * Live emptiness of the current (possibly edited) author value. Delegates to the shared
     * {@link authorIsEmpty} predicate (also used for the unrendered/publish case via
     * `nativeWidgetEmptyCheckers`) so there is a single source of truth.
     */
    private get isEmptyValue(): boolean {
        return (
            !this.showMixed('freetext') &&
            !this.showMixed('author') &&
            authorIsEmpty(this.author?.freetext, this.author?.author)
        );
    }

    /**
     * recalculate the input status based on the `required` configuration.
     * `mandatory` blocks saving (INVALID), `mandatoryForPublish` is only a warning (stays VALID).
     */
    private updateStatus(): void {
        const empty = this.isEmptyValue;
        this.isEmpty.next(empty);
        const status: InputStatus =
            this.isRequired === 'mandatory' && empty && this.isEditingActive ? 'INVALID' : 'VALID';
        this.status.next(status);
    }

    /**
     * called by the editor when the user tries to save while required fields are missing,
     * so the hint gets displayed.
     */
    showMissingRequired(): void {
        this.showMissingRequiredHint = true;
    }

    onBlur(): void {
        this.showMissingRequiredHint = true;
    }

    /**
     * whether the missing-required hint/marker should currently be shown to the user
     */
    get isMissingRequired(): boolean {
        return !!this.isRequired && this.isEmptyValue;
    }

    /** the real widget in bulk mode, so the container shows the switch */
    get containerWidget(): Partial<Widget> | null {
        return this.isBulk && this.widget ? this.widget : this.requiredWidget;
    }

    /**
     * "fake" widget passed to the container so it shows the required marker via the existing
     * `widget.definition.isRequired` logic. Returns a stable, cached instance.
     */
    get requiredWidget() {
        if (!this.isRequired) {
            return null;
        }
        if (!this._requiredWidget) {
            this._requiredWidget = {
                definition: { id: 'author', isRequired: this.isRequired, isExtended: false },
                meetsDynamicCondition: new BehaviorSubject(true),
            } as Partial<Widget>;
        }
        return this._requiredWidget;
    }

    static async openContributorDialog(
        mdsEditorInstanceService: MdsEditorInstanceService,
        dialogs: DialogsService,
    ): Promise<Node[] | null> {
        // update props before switching to contributor to keep local changes
        const nodes = mdsEditorInstanceService.nodes$.value;
        for (const node of nodes) {
            const values = await mdsEditorInstanceService.getValues(node, false);
            Object.keys(values).forEach((key) => (node.properties[key] = values[key]));
        }
        const dialogRef = await dialogs.openContributorsDialog({ nodes });
        return dialogRef
            .afterClosed()
            .pipe(
                tap((result) => {
                    if (result?.length) {
                        mdsEditorInstanceService.updateNodes(result);
                    }
                }),
                map((result) => result ?? null),
            )
            .toPromise();
    }

    async setVCardAuthor(author: boolean) {
        if (author) {
            this.author.author = await this.iamApi.getCurrentUserVCard();
        } else {
            this.author.author = new VCard();
        }
        this.onAuthorChange();
    }

    /** in bulk mode only the edited parts are written */
    async getValues(values: Values, node: Node): Promise<Values> {
        if (!this.author) {
            return values;
        }
        if (!this.isEditingActive) {
            return values;
        }
        return applyAuthorValues(values, node, {
            freetext: !this.isBulk || this.freetextChanged ? this.author.freetext : undefined,
            author: !this.isBulk || this.authorChanged ? this.author.author : undefined,
        });
    }
    private async updateValues(nodes: Node[]) {
        if (this.hasChanges.value) {
            return;
        }
        this._nodes = nodes;
        void this.refreshTabs();
    }

    public async refreshTabs() {
        if (this._nodes?.length) {
            let freetext = Array.from(
                new Set(
                    this._nodes.map(
                        (n) => n.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0],
                    ),
                ),
            );
            let author = Array.from(
                new Set(
                    this._nodes.map(
                        (n) =>
                            n.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0],
                    ),
                ),
            );
            this.mixedFreetext = freetext.length !== 1;
            this.mixedAuthor = author.length !== 1;
            this.freetextTouched = false;
            this.authorTouched = false;
            if (freetext.length !== 1) {
                freetext = null;
            }
            let authorVCard = new VCard();
            if (author.length !== 1) {
                author = null;
            } else {
                authorVCard = new VCard(author[0]);
            }
            this.userAuthor =
                authorVCard?.uid &&
                authorVCard?.uid === (await this.iamApi.getCurrentUserVCard()).uid;
            this.author = {
                freetext: freetext?.[0] ?? '',
                author: authorVCard,
            };
            // set default tab based on config
            if (!this.author.freetext?.trim() && !this.author.author?.getDisplayName().trim()) {
                if (this.attributes?.defaulttab) {
                    const tab = DefaultTab[this.attributes.defaulttab as DefaultTab];
                    if (tab === DefaultTab.vcard) {
                        this.authorTab = 1;
                    }
                }
            }
            // switch to author tab if no freetext but author exists
            if (!this.author.freetext?.trim() && this.author.author?.getDisplayName().trim()) {
                this.authorTab = 1;
            } else if (
                this.author.freetext?.trim() &&
                !this.author.author?.getDisplayName().trim()
            ) {
                this.authorTab = 0;
            }
            // read the required configuration from the template attribute, e.g.
            // `<author required="mandatory">` (analogous to `defaulttab`)
            const required = this.attributes?.required ?? this.attributes?.isrequired;
            if (required === 'mandatory' || required === 'mandatoryForPublish') {
                this.isRequired = required;
            }
            this.updateStatus();
            // deep copy the elements to compare state
            this.initialAuthor = {
                freetext: this.author.freetext,
                author: new VCard(this.author.author.toVCardString()),
            };
            // the refresh may be triggered from outside the zone (e.g. when the contributor dialog
            // closes), so the new values would otherwise only show up on the next user interaction
            this.changeDetectorRef.markForCheck();
            setTimeout(() => this.tabGroup.realignInkBar());
        }
    }

    async openContributorDialog() {
        const updatedNodes = await MdsEditorWidgetAuthorComponent.openContributorDialog(
            this.mdsEditorValues,
            this.dialogs,
        );
        if (updatedNodes?.length) {
            await this.updateValues(this.mdsEditorValues.nodes$.value);
        }
    }
}
