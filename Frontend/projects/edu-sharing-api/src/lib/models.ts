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
import { GetSuggestionsByNodeId$Params } from './api/fn/suggestions-v-1/get-suggestions-by-node-id';

export {
    About,
    Connector,
    ConnectorFileType,
    ConnectorList,
    CollectionEntries,
    ParentEntries,
    CollectionReference,
    Context,
    CollectionsType,
    CollectionsTypeConfig,
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
    MdsAiConfig,
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
    NodeEntry,
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
    Statistics,
    StatisticsGroup,
    StreamEntry,
    Tool,
    NodeSuggestionResponseDto,
    SuggestionResponseDto,
    Suggestion,
    Tools,
    CreateQaEntryDto,
    UpdateQaEntryDto,
    QaEntry,
    User,
    UserSimple,
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
