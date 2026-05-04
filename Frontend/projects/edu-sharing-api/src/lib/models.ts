// Reexport API models that are exposed by wrappers.
import { Acl } from './api/models/acl';
import { Group } from './api/models/group';
import { GroupProfile } from './api/models/group-profile';
import { MdsView } from './api/models/mds-view';
import { Mediacenter as MediacenterApi } from './api/models/mediacenter';
import { MediacenterProfileExtension } from './api/models/mediacenter-profile-extension';
import { Organization } from './api/models/organization';
import { User } from './api/models/user';

import { HttpErrorResponse } from '@angular/common/http';
import { SuggestionsV1Service } from './api/services/suggestions-v-1.service';

export type Mediacenter = MediacenterApi & {
    profile: GroupProfile & {
        mediacenter: MediacenterProfileExtension;
    };
};

export type SuggestionStatus = Parameters<SuggestionsV1Service['updateStatus']>[0]['status'];
export type SuggestionsByNodeIdParams = Parameters<
    SuggestionsV1Service['getSuggestionsByNodeId']
>[0];
export type MdsViewRelation = MdsView['rel'];
export type GenericAuthority = Organization | Group | User;
export type ApiErrorResponse = HttpErrorResponse & {
    readonly defaultPrevented: boolean;
    preventDefault: () => void;
};

export type NodePermissions = Acl;

/** Copy from Angular Material. */
export interface Sort {
    /** The id of the column being sorted. */
    active: string;
    /** The sort direction. */
    direction: 'asc' | 'desc' | '';
}

export { About } from './api/models/about';
export { Connector } from './api/models/connector';
export { ConnectorFileType } from './api/models/connector-file-type';
export { ConnectorList } from './api/models/connector-list';
export { CollectionEntries } from './api/models/collection-entries';
export { ParentEntries } from './api/models/parent-entries';
export { CollectionReference } from './api/models/collection-reference';
export { Context } from './api/models/context';
export { CollectionsType } from './api/models/collections-type';
export { CollectionsTypeConfig } from './api/models/collections-type-config';
export { Copy } from './api/models/copy';
export { CreateSuggestionRequestDto } from './api/models/create-suggestion-request-dto';
export { DashboardShortcut } from './api/models/dashboard-shortcut';
export { DashboardShortcutEntry } from './api/models/dashboard-shortcut-entry';
export { DefaultDashboardShortcut } from './api/models/default-dashboard-shortcut';
export { DefaultDashboardShortcutEntry } from './api/models/default-dashboard-shortcut-entry';
export { FeedbackData } from './api/models/feedback-data';
export { Acl } from './api/models/acl';
export { Ace } from './api/models/ace';
export { Authority } from './api/models/authority';
export { QrCode2Fa } from './api/models/qr-code-2-fa';
export { LicenseAgreement } from './api/models/license-agreement';
export { ManualRegistrationData } from './api/models/manual-registration-data';
export { HandleParam } from './api/models/handle-param';
export { NodePermissions as NodePermissionsGet } from './api/models/node-permissions';
export { NodePermissionInheritance } from './api/models/node-permission-inheritance';
export { Mds as MdsDefinition } from './api/models/mds';
export { MdsAiConfig } from './api/models/mds-ai-config';
export { MdsGroup } from './api/models/mds-group';
export { MdsQueryCriteria } from './api/models/mds-query-criteria';
export { MdsSort } from './api/models/mds-sort';
export { MdsSortColumn } from './api/models/mds-sort-column';
export { MdsSortDefault } from './api/models/mds-sort-default';
export { MdsValue } from './api/models/mds-value';
export { MdsView } from './api/models/mds-view';
export { UserEvent } from './api/models/user-event';
export { SearchResultEvent } from './api/models/search-result-event';
export { FeatureInfo } from './api/models/feature-info';
export { MdsWidget } from './api/models/mds-widget';
export { MdsWidgetCondition } from './api/models/mds-widget-condition';
export { MetadataSetInfo } from './api/models/metadata-set-info';
export { NotificationEventDto as Notification } from './api/models/notification-event-dto';
export { Config } from './api/models/config';
export { SubmissionFile } from './api/models/submission-file';
export { Values as ConfigValues } from './api/models/values';
export { ConfigTutorial } from './api/models/config-tutorial';
export { NotificationConfig } from './api/models/notification-config';
export { Assignment } from './api/models/assignment';
export { CreateAssignmentRequest } from './api/models/create-assignment-request';
export { Submission } from './api/models/submission';
export { Permission } from './api/models/permission';
export { PermissionRequest } from './api/models/permission-request';
export { AssignmentFile } from './api/models/assignment-file';
export { AssignmentFileRequest } from './api/models/assignment-file-request';
export { Node } from './api/models/node';
export { NodeEntry } from './api/models/node-entry';
export { NodeEntries } from './api/models/node-entries';
export { NodeVersion } from './api/models/node-version';
export { NodeVersionEntries } from './api/models/node-version-entries';
export { NodeVersionRef } from './api/models/node-version-ref';
export { NodeVersionRefEntries } from './api/models/node-version-ref-entries';
export { NodeRef } from './api/models/node-ref';
export { NodeStats } from './api/models/node-stats';
export { ConfigThemeColor } from './api/models/config-theme-color';
export { Organization } from './api/models/organization';
export { Group } from './api/models/group';
export { GroupProfile } from './api/models/group-profile';
export { UserProfileEdit } from './api/models/user-profile-edit';
export { MediacenterProfileExtension } from './api/models/mediacenter-profile-extension';
export { Pagination } from './api/models/pagination';
export { Person } from './api/models/person';
export { NodeRelationData } from './api/models/node-relation-data';
export { ReferenceEntries } from './api/models/reference-entries';
export { OAuth2Consent } from './api/models/o-auth-2-consent';
export { SearchResultNode as SearchResults } from './api/models/search-result-node';
export { SearchResultInvite } from './api/models/search-result-invite';
export { InviteEvent } from './api/models/invite-event';
export { SearchParameters } from './api/models/search-parameters';
export { Repo as Repository } from './api/models/repo';
export { RefDashboardShortcutEntry } from './api/models/ref-dashboard-shortcut-entry';
export { RefDashboardShortcut } from './api/models/ref-dashboard-shortcut';
export { ShortcutConfig } from './api/models/shortcut-config';
export { ShortcutConfigEntry } from './api/models/shortcut-config-entry';
export { Statistics } from './api/models/statistics';
export { StatisticsGroup } from './api/models/statistics-group';
export { StreamEntry } from './api/models/stream-entry';
export { Tool } from './api/models/tool';
export { NodeSuggestionResponseDto } from './api/models/node-suggestion-response-dto';
export { SuggestionResponseDto } from './api/models/suggestion-response-dto';
export { SuggestionNode } from './api/models/suggestion-node';
export { Suggestion } from './api/models/suggestion';
export { Tools } from './api/models/tools';
export { CreateQaEntryDto } from './api/models/create-qa-entry-dto';
export { UpdateQaEntryDto } from './api/models/update-qa-entry-dto';
export { QaEntry } from './api/models/qa-entry';
export { QaEntryResponseDto } from './api/models/qa-entry-response-dto';
export { User } from './api/models/user';
export { UserSimple } from './api/models/user-simple';
export { UserProfile } from './api/models/user-profile';
export { UserQuota } from './api/models/user-quota';
export { UserStats } from './api/models/user-stats';
export { UserStatsGroup } from './api/models/user-stats-group';
export { UserStatus } from './api/models/user-status';
export { WebsiteInformation } from './api/models/website-information';
export { RegisterInformation } from './api/models/register-information';
export { PrimaryLogin } from './api/models/primary-login';
export { ScopeLogin } from './api/models/scope-login';
export { OAuthEntry } from './api/models/o-auth-entry';
export { RepositoryConfig } from './api/models/repository-config';
export { RepositoryMessage } from './api/models/repository-message';
