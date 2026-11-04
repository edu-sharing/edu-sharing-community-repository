import { Pipe, PipeTransform } from '@angular/core';
import { Swimlane } from '../types/swimlane';
import { retrieveSwimlaneCompletion } from '../utils/swimlane-completion-util';

@Pipe({
    name: 'filterUnfinishedSwimlanes',
    standalone: true,
})
export class FilterUnfinishedSwimlanesPipe implements PipeTransform {
    /**
     * Drops the swimlanes readers do not see as they lack any renderable widget; editors see all.
     * The trigger only makes the pure pipe recompute after the swimlanes changed in place.
     */
    transform(
        swimlanes: Swimlane[],
        editMode: boolean,
        isRenderableWidgetType: (widgetType: string) => boolean,
        trigger: number,
    ): Swimlane[] {
        if (editMode || !swimlanes?.length || !trigger) {
            return swimlanes ?? [];
        }
        return swimlanes.filter(
            (swimlane: Swimlane): boolean =>
                retrieveSwimlaneCompletion(swimlane, isRenderableWidgetType) !== 'unfinished',
        );
    }
}
