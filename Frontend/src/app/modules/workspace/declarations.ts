import { WorkspaceMainComponent } from './workspace.component';
import { WorkspaceTreeComponent } from './tree/tree.component';
import { WorkspaceSubTreeComponent } from './sub-tree/sub-tree.component';
import { WorkspaceExplorerComponent } from './explorer/explorer.component';
import { WorkspaceMetadataComponent } from './metadata/metadata.component';
import { WorkspaceRoutingComponent } from './workspace-routing.component';
import { WorkspaceWorkflowComponent } from './workflow/workflow.component';
import { WorkspaceMetadataBlockComponent } from './metadata/metadata-block/metadata-block.component';
import { WorkflowListComponent } from './workflow/workflow-history/workflow-history.component';

export const DECLARATIONS_WORKSPACE = [
    WorkspaceRoutingComponent,
    WorkspaceMainComponent,
    WorkspaceTreeComponent,
    WorkspaceSubTreeComponent,
    WorkspaceExplorerComponent,
    WorkspaceMetadataComponent,
    WorkspaceMetadataBlockComponent,
    WorkspaceWorkflowComponent,
    WorkflowListComponent,
];
