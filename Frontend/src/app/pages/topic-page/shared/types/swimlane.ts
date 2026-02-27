import { GridTile } from './grid-tile';
import { SwimlaneBackgroundShape } from './swimlane-background-shape';

export interface Swimlane {
    id?: string;
    type?: string;
    heading?: string;
    grid?: GridTile[];
    backgroundColor?: string;
    backgroundShape?: SwimlaneBackgroundShape;
    backgroundShapeMirrored?: boolean;
}
