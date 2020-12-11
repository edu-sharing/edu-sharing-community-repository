import { Component, OnInit } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from '../../../../../core-module/rest/data-object';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestIamService } from '../../../../../core-module/rest/services/rest-iam.service';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { VCard } from '../../../../../core-module/ui/VCard';
import { MainNavService } from '../../../../services/main-nav.service';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidget } from '../../mds-editor-view/mds-editor-view.component';
import { Values } from '../../types';

export interface AuthorData {
    freetext: string;
    author: VCard;
}

@Component({
    selector: 'app-mds-editor-widget-author',
    templateUrl: './mds-editor-widget-author.component.html',
    styleUrls: ['./mds-editor-widget-author.component.scss'],
})
export class MdsEditorWidgetAuthorComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
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
        private mdsEditorValues: MdsEditorInstanceService,
        private iamApi: RestIamService,
        private mainNavService: MainNavService,
        public ui: UIService,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$
            .filter((n) => n != null)
            .subscribe((nodes) => {
                this._nodes = nodes;
                if (nodes?.length) {
                    let freetext = Array.from(
                        new Set(
                            nodes.map(
                                (n) => n.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0],
                            ),
                        ),
                    );
                    let author = Array.from(
                        new Set(
                            nodes.map(
                                (n) =>
                                    n.properties[
                                        RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR
                                    ]?.[0],
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
                        authorVCard?.uid === this.iamApi.getCurrentUserVCard().uid;
                    this.author = {
                        freetext: freetext?.[0],
                        author: authorVCard,
                    };
                    // switch to author tab if no freetext but author exists
                    if (
                        !this.author.freetext?.trim() &&
                        this.author.author?.getDisplayName().trim()
                    ) {
                        this.authorTab = 1;
                    }
                    // deep copy the elements to compare state
                    this.initialAuthor = {
                        freetext: this.author.freetext,
                        author: new VCard(this.author.author.toVCardString()),
                    };
                }
            });
    }
    onChange(): void {
        this.hasChanges.next(
            this.initialAuthor.freetext !== this.author.freetext ||
                this.initialAuthor.author.getDisplayName() !== this.author.author.getDisplayName(),
        );
    }

    openContributorDialog(): void {
        this.mainNavService.getDialogs().nodeContributor = this._nodes[0];
        this.mainNavService
            .getDialogs()
            .nodeContributorChange.first()
            .subscribe((n) => {
                // @TODO: we may need to reload the node inside the mds?!
            });
    }

    setVCardAuthor(author: boolean): void {
        if (author) {
            this.author.author = this.iamApi.getCurrentUserVCard();
        } else {
            this.author.author = new VCard();
        }
        this.onChange();
    }

    async getValues(values: Values): Promise<Values> {
        values[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [this.author.freetext];
        if (!values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]) {
            values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = [];
        }
        values[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = [
            this.author.author.toVCardString(),
        ];
        return values;
    }
}
