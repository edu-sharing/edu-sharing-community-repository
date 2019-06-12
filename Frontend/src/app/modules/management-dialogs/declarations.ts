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

export const DECLARATIONS_MANAGEMENT_DIALOGS = [
  WorkspaceLicenseComponent,
  WorkspaceLtiToolsComponent,
  WorkspaceLtiToolConfigsComponent,
  WorkspaceManagementDialogsComponent,
  WorkspaceCreateLtiComponent,
  WorkspaceFileUploadComponent,
  WorkspaceFileUploadSelectComponent,
  WorkspaceContributorComponent,
  NodeReportComponent,
  CommentsListComponent,
  AddStreamComponent,
  NodeVariantComponent,
  NodeTemplateComponent
];
