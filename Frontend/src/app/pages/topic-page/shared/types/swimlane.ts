import { GridTile } from './grid-tile';
import { SwimlaneBackgroundShape } from './swimlane-background-shape';
import { SwimlaneRepeat } from './swimlane-repeat';

export interface Swimlane {
    id?: string;
    type?: string;
    heading?: string;
    grid?: GridTile[];
    // page templates only: stands for one swimlane per item the rule resolves to, and is
    // replaced by its result when a variant is created from the template
    repeat?: SwimlaneRepeat;
    backgroundColor?: string;
    backgroundShape?: SwimlaneBackgroundShape;
    backgroundShapeMirrored?: boolean;
}
