// Reexport API models that are exposed by wrappers.

export {
    About,
    Connector,
    ConnectorList,
    Mds as MdsDefinition,
    MdsGroup,
    MdsSort,
    MdsValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    FeedbackData,
    Node,
    NodeEntries,
    NodeRef,
    Organization,
    Person,
    RelationData,
    SearchResultNode as SearchResults,
    StreamEntry,
    UserProfile,
    UserQuota,
    UserStatus,
    WebsiteInformation,
    LicenseAgreement,
    RegisterInformation,
} from './api/models';
import { HttpErrorResponse } from '@angular/common/http';
import { MdsView } from './api/models';

export type MdsViewRelation = MdsView['rel'];

export type ApiErrorResponse = HttpErrorResponse & {
    readonly defaultPrevented: boolean;
    preventDefault: () => void;
};
