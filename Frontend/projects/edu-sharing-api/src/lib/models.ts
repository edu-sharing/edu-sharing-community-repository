// Reexport API models that are exposed by wrappers.

import {
    Acl,
    Group,
    GroupProfile,
    MdsView,
    Mediacenter as MediacenterApi,
    MediacenterProfileExtension,
    Organization,
    Person,
    User,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { SuggestionsV1Service } from './api/services/suggestions-v-1.service';

export {
    About,
    Connector,
    ConnectorFileType,
    ConnectorList,
    CollectionReference,
    Context,
    DashboardShortcut,
    DashboardShortcutEntry,
    DefaultDashboardShortcut,
    DefaultDashboardShortcutEntry,
    FeedbackData,
    Acl,
    Ace,
    Authority,
    QrCode2Fa,
    LicenseAgreement,
    ManualRegistrationData,
    HandleParam,
    NodePermissions as NodePermissionsGet,
    Mds as MdsDefinition,
    MdsGroup,
    MdsQueryCriteria,
    MdsSort,
    MdsSortColumn,
    MdsSortDefault,
    MdsValue,
    MdsView,
    FeatureInfo,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    NotificationEventDto as Notification,
    Config,
    Values as ConfigValues,
    ConfigTutorial,
    NotificationConfig,
    Node,
    NodeEntries,
    NodeVersion,
    NodeVersionEntries,
    NodeVersionRef,
    NodeVersionRefEntries,
    NodeRef,
    NodeStats,
    ConfigThemeColor,
    Organization,
    Group,
    GroupProfile,
    UserProfileEdit,
    MediacenterProfileExtension,
    Pagination,
    Person,
    RelationData,
    ReferenceEntries,
    SearchResultNode as SearchResults,
    SearchParameters,
    Repo as Repository,
    RefDashboardShortcutEntry,
    RefDashboardShortcut,
    ShortcutConfig,
    ShortcutConfigEntry,
    Statistics,
    StatisticsGroup,
    StreamEntry,
    Tool,
    NodeSuggestionResponseDto,
    SuggestionResponseDto,
    Suggestion,
    Tools,
    User,
    UserProfile,
    UserQuota,
    UserStatus,
    WebsiteInformation,
    RegisterInformation,
} from './api/models';
export type Mediacenter = MediacenterApi & {
    profile: GroupProfile & {
        mediacenter: MediacenterProfileExtension;
    };
};
export type SuggestionStatus = Parameters<SuggestionsV1Service['updateStatus']>[0]['status'];
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
