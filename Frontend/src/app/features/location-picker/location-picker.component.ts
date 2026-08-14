import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Node, RestConstants, SessionStorageService } from 'ngx-edu-sharing-api';
import { notNull } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, of } from 'rxjs';
import { catchError, filter, map } from 'rxjs/operators';
import { ParentList, RestNodeService } from '../../core-module/core.module';
import { DialogsService } from '../dialogs/dialogs.service';
import { Toast } from '../../services/toast';
import { BreadcrumbsService } from '../../shared/components/breadcrumbs/breadcrumbs.service';
import { SharedModule } from '../../shared/shared.module';

/**
 * Picks a folder as a storage location, showing it as a breadcrumb path. Offers to persist the
 * choice as the user's default location once a location was picked.
 */
@Component({
    selector: 'es-location-picker',
    templateUrl: './location-picker.component.html',
    styleUrls: ['./location-picker.component.scss'],
    providers: [BreadcrumbsService],
    imports: [SharedModule],
})
export class LocationPickerComponent {
    private breadcrumbsService = inject(BreadcrumbsService);
    private dialogs = inject(DialogsService);
    private nodeService = inject(RestNodeService);
    private storageService = inject(SessionStorageService);
    private toast = inject(Toast);

    @Input() set parent(parent: Node) {
        this.parent$.next(parent);
    }
    /** whether picking a location offers to persist it as the user's default */
    @Input() allowSaveAsDefault = true;
    @Output() parentChange = new EventEmitter<Node>();

    protected readonly parent$ = new BehaviorSubject<Node>(null);
    protected breadcrumbs: {
        homeLabel: string;
        homeIcon: string;
    };
    protected showSaveParent = false;
    protected saveParent = false;

    constructor() {
        this.parent$
            .pipe(
                takeUntilDestroyed(),
                filter((p) => !!p),
            )
            .subscribe((parent) => {
                this.breadcrumbs = null;
                this.breadcrumbsService.setNodePath(null);
                this.getBreadcrumbs(parent)
                    .pipe(filter(notNull))
                    .subscribe((breadcrumbs) => {
                        this.breadcrumbs = breadcrumbs;
                        this.breadcrumbsService.setNodePath(breadcrumbs.nodes);
                    });
            });
    }

    async chooseParent() {
        const dialogRef = await this.dialogs.openFileChooserDialog({
            pickDirectory: true,
            title: 'WORKSPACE.CHOOSE_LOCATION_TITLE',
            subtitle: 'WORKSPACE.CHOOSE_LOCATION_DESCRIPTION',
        });
        dialogRef.afterClosed().subscribe((nodes) => {
            if (nodes) {
                this.parentSelected(nodes[0]);
            }
        });
    }

    private parentSelected(parent: Node) {
        this.showSaveParent = this.allowSaveAsDefault;
        this.parent$.next(parent);
        this.parentChange.emit(parent);
    }

    protected async setSaveParent(status: boolean) {
        if (status) {
            await this.storageService.set('defaultInboxFolder', this.parent$.value.ref.id);
            this.toast.toast('TOAST.STORAGE_LOCATION_SAVED', { name: this.parent$.value.name });
        } else {
            await this.storageService.delete('defaultInboxFolder');
            this.toast.toast('TOAST.STORAGE_LOCATION_RESET');
        }
    }

    private getBreadcrumbs(node: Node) {
        if (node && node.ref.id !== RestConstants.USERHOME) {
            return this.nodeService.getNodeParents(node.ref.id).pipe(
                map((parentList) => this.getBreadcrumbsByParentList(parentList)),
                catchError(() =>
                    of(
                        this.getBreadcrumbsByParentList({
                            nodes: [node],
                            pagination: null,
                            scope: 'UNKNOWN',
                        }),
                    ),
                ),
            );
        } else {
            return of(null);
        }
    }

    private getBreadcrumbsByParentList(parentList: ParentList) {
        const nodes = parentList.nodes.reverse();
        switch (parentList.scope) {
            case 'MY_FILES':
            // api will return null if fullPath was requested (i.e. as admin)
            case null:
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.MY_FILES',
                    homeIcon: 'person',
                };
            case 'SHARED_FILES':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.SHARED_FILES',
                    homeIcon: 'group',
                };
            case 'UNKNOWN':
                return {
                    nodes,
                    homeLabel: 'WORKSPACE.RESTRICTED_FOLDER',
                    homeIcon: 'folder',
                };
            default:
                console.warn(`Unknown scope "${parentList.scope}"`);
                return {
                    nodes,
                    homeLabel: null,
                    homeIcon: null,
                };
        }
    }
}
