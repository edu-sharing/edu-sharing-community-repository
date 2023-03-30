// Reexport API models that are exposed by wrappers.

export {
    About,
    Connector,
    ConnectorList,
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
    Person,
    RelationData,
    SearchResultNode as SearchResults,
    StreamEntry,
    Tool,
    Tools,
    UserProfile,
    UserQuota,
    UserStatus,
    WebsiteInformation,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { Acl, MdsView } from './api/models';

export type MdsViewRelation = MdsView['rel'];

export type ApiErrorResponse = HttpErrorResponse & {
    readonly defaultPrevented: boolean;
    preventDefault: () => void;
};

export type NodePermissions = Acl;
