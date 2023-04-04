// Reexport API models that are exposed by wrappers.

export {
    About,
    Connector,
    ConnectorList,
    CollectionReference,
    FeedbackData,
    LicenseAgreement,
    Mds as MdsDefinition,
    MdsGroup,
    MdsQueryCriteria,
    MdsSort,
    MdsSortColumn,
    MdsValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    Node,
    NodeEntries,
    NodeRef,
    Organization,
    Group,
    Pagination,
    Person,
    RelationData,
    SearchResultNode as SearchResults,
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
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { Group, MdsView, Organization, Person, User } from './api/models';

export type MdsViewRelation = MdsView['rel'];
export type GenericAuthority = Organization | Group | User;
export type ApiErrorResponse = HttpErrorResponse & {
    readonly defaultPrevented: boolean;
    preventDefault: () => void;
};
