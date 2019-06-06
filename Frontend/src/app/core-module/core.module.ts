import {NgModule} from '@angular/core';
import {RestUtilitiesService} from "./rest/services/rest-utilities.service";
import {RestLocatorService} from "./rest/services/rest-locator.service";
import {RestCollectionService} from "./rest/services/rest-collection.service";
import {RestConnectorsService} from "./rest/services/rest-connectors.service";
import {RestToolService} from "./rest/services/rest-tool.service";
import {RestNodeService} from "./rest/services/rest-node.service";
import {RestConnectorService} from "./rest/services/rest-connector.service";
import {RestNetworkService} from "./rest/services/rest-network.service";
import {RestMdsService} from "./rest/services/rest-mds.service";
import {RestOrganizationService} from "./rest/services/rest-organization.service";
import {RestCommentsService} from "./rest/services/rest-comments.service";
import {RestIamService} from "./rest/services/rest-iam.service";
import {RestArchiveService} from "./rest/services/rest-archive.service";
import {RestAdminService} from "./rest/services/rest-admin.service";
import {RestRegisterService} from "./rest/services/rest-register.service";
import {RestSearchService} from "./rest/services/rest-search.service";
import {RestStreamService} from "./rest/services/rest-stream.service";
import {RestUsageService} from "./rest/services/rest-usage.service";
import {RestSharingService} from "./rest/services/rest-sharing.service";
import {DebugResultPipe} from "./rest/pipes/debug-result.pipe";
import {FrameEventsService} from "./rest/services/frame-events.service";
import {SessionStorageService} from "./rest/services/session-storage.service";
import {TemporaryStorageService} from "./rest/services/temporary-storage.service";
import {UIService} from "./rest/services/ui.service";
import {CoreBridgeModule} from "../core-bridge-module/core.bridge.module";
import {ConfigurationService} from "./rest/services/configuration.service";
import {DialogButton} from "./ui/dialog-button";
import {UIConstants} from "./ui/ui-constants";

@NgModule({
    imports: [
        CoreBridgeModule,
    ],
    declarations: [
        DebugResultPipe
    ],
    providers:[
        RestLocatorService,
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
        RestRegisterService,
        RestStreamService,
        RestUsageService,
        RestOrganizationService,
        RestCommentsService,
        RestToolService,
        RestUtilitiesService,
        RestSharingService,
        FrameEventsService,
        SessionStorageService,
        ConfigurationService,
        TemporaryStorageService,
        UIService
    ]
})
export class CoreModule {}

export * from "./rest/data-object";
export * from "./rest/rest-constants";
export * from "./rest/request-object";
export * from "./rest/rest-helper";
export {DialogButton} from "./ui/dialog-button";
export {ListItem} from "./ui/list-item";
export {UIConstants} from "./ui/ui-constants";
export * from "./rest/configuration-helper";
export * from "./rest/services/configuration.service";
export * from "./rest/services/rest-locator.service";
export * from "./rest/services/frame-events.service";
export * from "./rest/services/session-storage.service";
export * from "./rest/services/temporary-storage.service";
export {UIService} from "./rest/services/ui.service";
export {RestUtilitiesService} from "./rest/services/rest-utilities.service";
export {RestCollectionService} from "./rest/services/rest-collection.service";
export {RestConnectorsService} from "./rest/services/rest-connectors.service";
export {RestToolService} from "./rest/services/rest-tool.service";
export {RestNodeService} from "./rest/services/rest-node.service";
export {RestConnectorService} from "./rest/services/rest-connector.service";
export {RestNetworkService} from "./rest/services/rest-network.service";
export {RestMdsService} from "./rest/services/rest-mds.service";
export {RestOrganizationService} from "./rest/services/rest-organization.service";
export {RestCommentsService} from "./rest/services/rest-comments.service";
export {RestIamService} from "./rest/services/rest-iam.service";
export {RestArchiveService} from "./rest/services/rest-archive.service";
export {RestAdminService} from "./rest/services/rest-admin.service";
export {RestRegisterService} from "./rest/services/rest-register.service";
export {RestSearchService} from "./rest/services/rest-search.service";
export {RestStreamService} from "./rest/services/rest-stream.service";
export {RestUsageService} from "./rest/services/rest-usage.service";
export {RestSharingService} from "./rest/services/rest-sharing.service";
export {DebugResultPipe} from "./rest/pipes/debug-result.pipe";
