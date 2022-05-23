import {WorkspaceManagementDialogsComponent} from "../management-dialogs/management-dialogs.component";
import {WorkspaceLicenseComponent} from "./license/license.component";
import {WorkspaceLtiToolsComponent} from "./lti-tools/lti-tools.component";
import {WorkspaceLtiToolConfigsComponent} from "./lti-tool-configs/lti-tool-configs.component";
import {WorkspaceCreateLtiComponent} from "./create-lti/create-lti.component";
import {WorkspaceFileUploadComponent} from "./file-upload/file-upload.component";
import {WorkspaceFileUploadSelectComponent} from "./file-upload-select/file-upload-select.component";
import {WorkspaceContributorComponent} from "./contributor/contributor.component";
import {NodeReportComponent} from "./node-report/node-report.component";
import {AddStreamComponent} from "./add-stream/add-stream.component";
import {NodeTemplateComponent} from "./node-template/node-template.component";
import {NodeVariantComponent} from './node-variant/node-variant.component';
import {CommentsListComponent} from "./node-comments/comments-list/comments-list.component";
import {ViewCollectionFeedbackComponent} from './view-collection-feedback/view-collection-feedback.component';
import {SimpleEditDialogComponent} from './simple-edit-dialog/simple-edit-dialog.component';
import {SimpleEditMetadataComponent} from './simple-edit-dialog/simple-edit-metadata/simple-edit-metadata.component';
import {SimpleEditInviteComponent} from './simple-edit-dialog/simple-edit-invite/simple-edit-invite.component';
import {SimpleEditLicenseComponent} from './simple-edit-dialog/simple-edit-license/simple-edit-license.component';
import {LicenseSourceComponent} from "./license/license-source/license-source.component";
import {MapLinkComponent} from './map-link/map-link.component';
import {SignupGroupComponent} from './signup-group/signup-group.component';
import {NodeSearchSelectorComponent} from './node-search-selector/node-search-selector.component';
import {NodeRowComponent} from './node-search-selector/node-row/node-row.component';
import {
    NodeRelationManagerComponent
} from './node-relation-manager/node-relation-manager.component';

export const DECLARATIONS_MANAGEMENT_DIALOGS = [
  WorkspaceLicenseComponent,
  LicenseSourceComponent,
  WorkspaceLtiToolsComponent,
  WorkspaceLtiToolConfigsComponent,
  WorkspaceManagementDialogsComponent,
  WorkspaceCreateLtiComponent,
  WorkspaceFileUploadComponent,
  WorkspaceFileUploadSelectComponent,
  WorkspaceContributorComponent,
  NodeReportComponent,
  NodeRelationManagerComponent,
  CommentsListComponent,
  NodeSearchSelectorComponent,
  NodeRowComponent,
  AddStreamComponent,
  NodeVariantComponent,
  MapLinkComponent,
  NodeTemplateComponent,
  ViewCollectionFeedbackComponent,
  SimpleEditDialogComponent,
  SimpleEditMetadataComponent,
  SimpleEditLicenseComponent,
  SimpleEditInviteComponent,
  SignupGroupComponent,
];
