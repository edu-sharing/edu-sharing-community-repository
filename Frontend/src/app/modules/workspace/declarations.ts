import { WorkspaceMainComponent } from './workspace.component';
import { WorkspaceTreeComponent } from './tree/tree.component';
import { WorkspaceSubTreeComponent } from './sub-tree/sub-tree.component';
import { WorkspaceExplorerComponent } from './explorer/explorer.component';
import { WorkspaceMetadataComponent } from './metadata/metadata.component';
import { WorkspaceRoutingComponent } from './workspace-routing.component';
import { WorkspaceMetadataBlockComponent } from './metadata/metadata-block/metadata-block.component';

export const DECLARATIONS_WORKSPACE = [
    WorkspaceRoutingComponent,
    WorkspaceMainComponent,
    WorkspaceTreeComponent,
    WorkspaceSubTreeComponent,
    WorkspaceExplorerComponent,
    WorkspaceMetadataComponent,
    WorkspaceMetadataBlockComponent,
];
