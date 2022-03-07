// Reexport API models that are exposed by wrappers.

export {
    About,
    Mds as MdsDefinition,
    MdsGroup,
    MdsSort,
    MdsValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
    MetadataSetInfo,
    Node,
    SearchResultNode as SearchResults,
} from './api/models';
import { MdsView } from './api/models';

export type MdsViewRelation = MdsView['rel'];
