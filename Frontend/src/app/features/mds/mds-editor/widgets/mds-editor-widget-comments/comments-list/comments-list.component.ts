import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { HOME_REPOSITORY, NetworkService, Node, RestConstants } from 'ngx-edu-sharing-api';
import { OptionItem, UIAnimation } from 'ngx-edu-sharing-ui';
import { SharedModule } from 'src/app/shared/shared.module';
import { RestCommentsService } from '../../../../../../core-module/rest/services/rest-comments.service';
import { RestConnectorService } from '../../../../../../core-module/rest/services/rest-connector.service';
import { RestIamService } from '../../../../../../core-module/rest/services/rest-iam.service';
import {
    Comment,
    Comments,
    LoginResult,
    User,
} from '../../../../../../core-module/rest/data-object';
import { DialogsService } from '../../../../../dialogs/dialogs.service';
import { Toast } from '../../../../../../services/toast';
import { YES_OR_NO } from '../../../../../dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { first } from 'rxjs/operators';
import { isString } from 'lodash-es';

@Component({
    selector: 'es-comments-list',
    templateUrl: 'comments-list.component.html',
    styleUrls: ['comments-list.component.scss'],
    imports: [SharedModule],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class CommentsListComponent {
    public _node: Node;
    isGuest: boolean;
    user: User;
    comments: Comment[];
    private edit: Comment[];
    options: OptionItem[];

    public newComment = '';
    public editComment: Comment = null;
    public editCommentText: string;
    isLoading: boolean;
    sending: boolean;
    hasPermission: boolean;
    isFromHomeRepo: boolean;

    @Input() mode: 'view' | 'edit' = 'edit';
    /**
     * render comments as html content or text
     */
    @Input() text: 'plain' | 'html' = 'plain';
    @Input() set node(node: Node | string) {
        if (isString(node)) {
            this._node = {
                ref: {
                    id: node,
                    repo: HOME_REPOSITORY,
                },
                access: [],
            } as Node;
        } else {
            this._node = node;
        }
        void this.refresh();
    }
    @Output() cancelComment = new EventEmitter<void>();
    @Output() loading = new EventEmitter<boolean>();
    /**
     * Some data has changed, may be a new, removed or edited comment
     */
    @Output() changeComment = new EventEmitter<void>();
    dropdown = -1;

    constructor(
        private commentsApi: RestCommentsService,
        private networkService: NetworkService,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private iam: RestIamService,
        private toast: Toast,
    ) {
        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
            this.isGuest = data.isGuest;
            if (!data.isGuest) {
                void this.iam.getCurrentUserAsync().then((data) => {
                    this.user = data.person;
                    this.hasPermission = this.connector.hasToolPermissionInstant(
                        RestConstants.TOOLPERMISSION_COMMENT_WRITE,
                    );
                });
            }
        });
    }
    saveEditComment() {
        this.loading.emit(true);
        this.commentsApi
            .editComment(this.editComment.ref.id, this.editCommentText.trim())
            .subscribe(
                () => {
                    this.loading.emit(false);
                    this.changeComment.emit();
                    this.editComment = null;
                    void this.refresh();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.loading.emit(false);
                },
            );
    }
    getOptions(comment: Comment) {
        let options: OptionItem[] = [];
        let isAuthor = this.user && this.user.authorityName == comment.creator.authorityName;
        if (this.mode === 'edit') {
            if (isAuthor) {
                options.push(
                    new OptionItem('NODE_COMMENTS.OPTION_EDIT', 'edit', () => {
                        this.editComment = comment;
                        this.editCommentText = comment.comment;
                    }),
                );
            }
            if (isAuthor || this._node.access.indexOf(RestConstants.ACCESS_WRITE) != -1) {
                options.push(
                    new OptionItem('NODE_COMMENTS.OPTION_DELETE', 'delete', async () => {
                        const dialogRef = await this.dialogs.openGenericDialog({
                            title: 'NODE_COMMENTS.DELETE_COMMENT',
                            message: 'NODE_COMMENTS.DELETE_COMMENT_MESSAGE',
                            buttons: YES_OR_NO,
                        });
                        dialogRef.afterClosed().subscribe((response) => {
                            if (response === 'YES') {
                                this.loading.emit(true);
                                this.toast.closeProgressSpinner();
                                this.commentsApi.deleteComment(comment.ref.id).subscribe(
                                    () => {
                                        void this.refresh();
                                        this.changeComment.emit();
                                        this.loading.emit(false);
                                    },
                                    (error: any) => {
                                        this.toast.error(error);
                                        this.loading.emit(false);
                                    },
                                );
                            }
                        });
                    }),
                );
            }
        }
        return options;
    }
    public canComment() {
        if (!this.isFromHomeRepo || this.isGuest || !this.user || !this.hasPermission) return false;
        return this._node.access.indexOf(RestConstants.ACCESS_COMMENT) !== -1;
    }
    public addComment() {
        if (!this.newComment.trim()) {
            this.toast.error(null, 'NODE_COMMENTS.COMMENT_EMPTY');
            return;
        }
        this.sending = true;
        this.loading.emit(true);
        this.commentsApi.addComment(this._node.ref.id, this.newComment.trim()).subscribe(
            () => {
                this.sending = false;
                this.loading.emit(false);
                this.changeComment.emit();
                this.newComment = '';
                void this.refresh();
            },
            (error: any) => {
                this.sending = false;
                this.toast.error(error);
                this.loading.emit(false);
            },
        );
    }
    public cancel() {
        this.cancelComment.emit();
    }

    public async refresh() {
        this.comments = null;
        if (!this._node) return;
        if (this.isLoading) {
            setTimeout(() => this.refresh(), 100);
            return;
        }
        this.isFromHomeRepo = await this.networkService
            .isFromHomeRepository(this._node)
            .pipe(first())
            .toPromise();
        if (!this.isFromHomeRepo) {
            this.isLoading = false;
            this.comments = [];
            return;
        }
        this.isLoading = true;
        this.commentsApi.getComments(this._node.ref.id, this._node.ref.repo).subscribe(
            (data: Comments) => {
                this.isLoading = false;
                this.comments = data && data.comments ? data.comments.reverse() : [];
            },
            (error: any) => {
                this.isLoading = false;
                this.toast.error(error);
            },
        );
    }
}
