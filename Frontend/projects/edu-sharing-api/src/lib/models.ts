// Reexport API models that are exposed by wrappers.

export {
    About,
    Connector,
    ConnectorList,
    FeedbackData,
    LicenseAgreement,
    ManualRegistrationData,
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
    Mediacenter,
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
    RegisterInformation,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { Acl, MdsView } from './api/models';

export type MdsViewRelation = MdsView['rel'];

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
