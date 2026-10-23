import { GridTile } from '../types/grid-tile';
import { Swimlane } from '../types/swimlane';

/**
 * How far a swimlane is filled: unfinished ones lack any renderable widget and are hidden from
 * readers, partial ones show their renderable widgets next to empty tiles.
 */
export type SwimlaneCompletion = 'complete' | 'partial' | 'unfinished';

/**
 * Rates a swimlane by how many of its grid tiles hold a widget the page can render.
 */
export const retrieveSwimlaneCompletion = (
    swimlane: Swimlane,
    isRenderableWidgetType: (widgetType: string) => boolean,
): SwimlaneCompletion => {
    const tiles: GridTile[] = swimlane.grid ?? [];
    const renderableTiles: number = tiles.filter(
        (tile: GridTile): boolean => !!tile.item && isRenderableWidgetType(tile.item),
    ).length;
    if (renderableTiles === 0) {
        return 'unfinished';
    }
    return renderableTiles === tiles.length ? 'complete' : 'partial';
};
