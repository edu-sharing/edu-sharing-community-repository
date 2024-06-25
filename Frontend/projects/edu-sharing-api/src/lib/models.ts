// Reexport API models that are exposed by wrappers.

export {
    About,
    Connector,
    ConnectorFileType,
    ConnectorList,
    CollectionReference,
    FeedbackData,
    LicenseAgreement,
    ManualRegistrationData,
    Mds as MdsDefinition,
    MdsGroup,
    MdsQueryCriteria,
    MdsSort,
    MdsSortColumn,
    MdsSortDefault,
    MdsValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    NotificationEventDto as Notification,
    ConfigTutorial,
    Mediacenter,
    NotificationConfig,
    Node,
    NodeEntries,
    NodeVersion,
    NodeVersionEntries,
    NodeVersionRef,
    NodeVersionRefEntries,
    NodeRef,
    NodeStats,
    Organization,
    Group,
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
    Tools,
    User,
    UserProfile,
    UserQuota,
    UserStatus,
    WebsiteInformation,
    RegisterInformation,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { Acl, Group, MdsView, Organization, Person, User } from './api/models';

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
