import { Injectable } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { WorkspaceManagementDialogsComponent } from './management-dialogs.component';

/**
 * Provides access to dialogs via a global component.
 *
 * @deprecated will be replaced by overlay-based dialogs available through `DialogsService`.
 */
@Injectable({
    providedIn: 'root',
})
export class ManagementDialogsService {
    private dialogsComponent: WorkspaceManagementDialogsComponent;

    constructor(private route: ActivatedRoute, private router: Router) {}

    getDialogsComponent(): WorkspaceManagementDialogsComponent {
        return this.dialogsComponent;
    }

    registerDialogsComponent(dialogsComponent: WorkspaceManagementDialogsComponent): void {
        this.dialogsComponent = dialogsComponent;
        this.subscribeChanges();
    }

    private subscribeChanges() {
        this.dialogsComponent.signupGroupChange.subscribe((value: boolean) => {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParamsHandling: 'merge',
                queryParams: {
                    signupGroup: value || null,
                },
            });
        });
        this.route.queryParams.subscribe((params: Params) => {
            this.dialogsComponent.signupGroup = params.signupGroup;
        });
    }
}
