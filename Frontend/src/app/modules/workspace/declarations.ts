import {WorkspaceMainComponent} from "./workspace.component";
import {WorkspaceTreeComponent} from "./tree/tree.component";
import {WorkspaceSubTreeComponent} from "./sub-tree/sub-tree.component";
import {WorkspaceExplorerComponent} from "./explorer/explorer.component";
import {WorkspaceMetadataComponent} from "./metadata/metadata.component";
import {WorkspaceAddFolder} from "./add-folder/add-folder.component";
import {WorkspaceShareComponent} from "./share/share.component";
import {WorkspaceShareChooseTypeComponent} from "./share/choose-type/choose-type.component";
import {WorkspacePermissionComponent} from "./share/permission/permission.component";
import {WorkspaceEditorComponent} from "./editor/editor.component";
import {WorkspaceRoutingComponent} from "./workspace-routing.component";
import {WorkspaceCreateConnector} from "./create-connector/create-connector.component";
import {WorkspaceHistoryComponent} from "./share/history/history.component";
import {WorkspaceShareLinkComponent} from "./share-link/share-link.component";
import {WorkspaceWorkflowComponent} from "./workflow/workflow.component";
import {WorkspaceUsageComponent} from './share/usage/usage.component';

export const DECLARATIONS_WORKSPACE = [
  WorkspaceRoutingComponent,
  WorkspaceMainComponent,
  WorkspaceTreeComponent,
  WorkspaceSubTreeComponent,
  WorkspaceExplorerComponent,
  WorkspaceMetadataComponent,
  WorkspaceAddFolder,
  WorkspaceShareComponent,
  WorkspaceShareLinkComponent,
  WorkspaceShareChooseTypeComponent,
  WorkspacePermissionComponent,
  WorkspaceUsageComponent,
  WorkspaceHistoryComponent,
  WorkspaceEditorComponent,
  WorkspaceCreateConnector,
  WorkspaceWorkflowComponent,
];
