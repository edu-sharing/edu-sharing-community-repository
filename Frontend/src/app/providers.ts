import {RestNodeService} from "./common/rest/services/rest-node.service";
import {RestMdsService} from "./common/rest/services/rest-mds.service";
import {RestIamService} from "./common/rest/services/rest-iam.service";
import {RestArchiveService} from "./common/rest/services/rest-archive.service";
import {RestConnectorService} from "./common/rest/services/rest-connector.service";
import {GwtInterfaceService} from "./common/services/gwt-interface.service";
import {RestSearchService} from "./common/rest/services/rest-search.service";
import {Toast} from "./common/ui/toast";
import {RestCollectionService} from "./common/rest/services/rest-collection.service";
import {RestUsageService} from "./common/rest/services/rest-usage.service";
import {TemporaryStorageService} from "./common/services/temporary-storage.service";
import {RestMetadataService} from "./common/rest/services/rest-metadata.service";
import {SessionStorageService} from "./common/services/session-storage.service";
import {RestOrganizationService} from "./common/rest/services/rest-organization.service";
import {UIService} from "./common/services/ui.service";
import {RestConnectorsService} from "./common/rest/services/rest-connectors.service";
import {ConfigurationService} from "./common/services/configuration.service";
import {FrameEventsService} from "./common/services/frame-events.service";
import {RestAdminService} from "./common/rest/services/rest-admin.service";
import {RestNetworkService} from "./common/rest/services/rest-network.service";
import {RestToolService} from "./common/rest/services/rest-tool.service";
import {PermissionNamePipe} from "./common/ui/permission-name.pipe";


export const PROVIDERS=[
  FrameEventsService,
  Toast,
  GwtInterfaceService,
  RestConnectorService,
  RestConnectorsService,
  RestArchiveService,
  RestNetworkService,
  RestIamService,
  RestAdminService,
  RestCollectionService,
  RestMdsService,
  RestNodeService,
  RestSearchService,
  RestUsageService,
  RestOrganizationService,
  RestToolService,
  TemporaryStorageService,
  PermissionNamePipe,
  RestMetadataService,
  SessionStorageService,
  ConfigurationService,
  UIService
];
