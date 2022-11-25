import { filter, tap } from 'rxjs/operators';
import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from '../../../../../core-module/rest/data-object';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestIamService } from '../../../../../core-module/rest/services/rest-iam.service';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { VCard } from '../../../../../core-module/ui/VCard';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { Values } from '../../../types/types';
import { MatTabGroup } from '@angular/material/tabs';
import { MainNavService } from '../../../../../main/navigation/main-nav.service';
import { DialogsService } from '../../../../dialogs/dialogs.service';

export interface AuthorData {
    freetext: string;
    author: VCard;
}

@Component({
    selector: 'es-mds-editor-widget-author',
    templateUrl: './mds-editor-widget-author.component.html',
    styleUrls: ['./mds-editor-widget-author.component.scss'],
})
export class MdsEditorWidgetAuthorComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: false,
        supportsBulk: false,
    };
    @ViewChild(MatTabGroup) tabGroup: MatTabGroup;
    @Input() showContributorDialog = true;
    _nodes: Node[];
    hasChanges = new BehaviorSubject<boolean>(false);
    authorTab = 0;
    author: AuthorData;
    /**
     * is the current editing user the real author (matched by id)
     */
    userAuthor: boolean;
    private initialAuthor: AuthorData;

    constructor(
        public mdsEditorValues: MdsEditorInstanceService,
        private iamApi: RestIamService,
        private mainNavService: MainNavService,
        public ui: UIService,
        private dialogs: DialogsService,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$.pipe(filter((n) => n != null)).subscribe((nodes) => {
            this.updateValues(nodes);
        });
        this.mdsEditorValues.values$.pipe(filter((v) => v != null)).subscribe((values) => {
            this.updateValues([{ properties: values }] as Node[]);
        });
    }
    onChange(): void {
        this.hasChanges.next(
            this.initialAuthor.freetext !== this.author.freetext ||
                this.initialAuthor.author.getDisplayName() !== this.author.author.getDisplayName(),
        );
    }

    static async openContributorDialog(
        mdsEditorInstanceService: MdsEditorInstanceService,
        dialogs: DialogsService,
    ): Promise<Node | null> {
        // update props before switching to contributor to keep local changes
        const nodes = mdsEditorInstanceService.nodes$.value;
        const values = await mdsEditorInstanceService.getValues(nodes[0], false);
        const node = nodes[0];
        Object.keys(values).forEach((key) => (node.properties[key] = values[key]));
        // this._nodes[0].properties = await this.getValues(this._nodes[0].properties, this._nodes[0]);
        const dialogRef = await dialogs.openContributorsDialog({ node });
        return dialogRef
            .afterClosed()
            .pipe(
                tap((result) => {
                    if (result) {
                        mdsEditorInstanceService.updateNodes([result]);
                    }
                }),
            )
            .toPromise();
    }

    async setVCardAuthor(author: boolean) {
        if (author) {
            this.author.author = await this.iamApi.getCurrentUserVCard();
        } else {
            this.author.author = new VCard();
        }
        this.onChange();
    }

    async getValues(values: Values, node: Node = null): Promise<Values> {
        values[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [this.author.freetext];
        // copy current value from node, replace only first entry (if it has multiple authors)
        values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] =
            node?.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR];
        if (!values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]) {
            values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = [''];
        }
        if (this.author.author.isValid()) {
            values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR][0] =
                this.author.author.toVCardString();
        } else if (values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR].length === 1) {
            values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = null;
        } else {
            delete values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR][0];
        }
        return values;
    }
    private async updateValues(nodes: Node[]) {
        this._nodes = nodes;
        if (nodes?.length) {
            let freetext = Array.from(
                new Set(
                    nodes.map((n) => n.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0]),
                ),
            );
            let author = Array.from(
                new Set(
                    nodes.map(
                        (n) =>
                            n.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0],
                    ),
                ),
            );
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
            // switch to author tab if no freetext but author exists
            if (!this.author.freetext?.trim() && this.author.author?.getDisplayName().trim()) {
                this.authorTab = 1;
            }
            // deep copy the elements to compare state
            this.initialAuthor = {
                freetext: this.author.freetext,
                author: new VCard(this.author.author.toVCardString()),
            };
            setTimeout(() => this.tabGroup.realignInkBar());
        }
    }

    async openContributorDialog() {
        const updatedNode = await MdsEditorWidgetAuthorComponent.openContributorDialog(
            this.mdsEditorValues,
            this.dialogs,
        );
        if (updatedNode) {
            await this.updateValues([updatedNode]);
        }
    }
}
