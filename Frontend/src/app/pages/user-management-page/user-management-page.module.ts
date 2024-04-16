import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { PermissionsAuthoritiesComponent } from './authorities/authorities.component';
import { PermissionsDeleteComponent } from './delete/delete.component';
import { ToolpermissionManagerComponent } from './toolpermission-manager/toolpermission-manager.component';
import { UserManagementPageRoutingModule } from './user-management-page-routing.module';
import { UserManagementPageComponent } from './user-management-page.component';

@NgModule({
    declarations: [
        UserManagementPageComponent,
        PermissionsAuthoritiesComponent,
        ToolpermissionManagerComponent,
        PermissionsDeleteComponent,
    ],
    imports: [SharedModule, UserManagementPageRoutingModule],
})
export class UserManagementPageModule {}
