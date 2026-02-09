import { Pipe, PipeTransform } from '@angular/core';
import { Swimlane } from '../types/swimlane';
import { GridTile } from '../types/grid-tile';

@Pipe({
    name: 'swimlaneSearchCount',
    standalone: true,
})
export class SwimlaneSearchCountPipe implements PipeTransform {
    /**
     * Returns the total search count of a swimlane grid.
     *
     * @param swimlane
     * @param searchCountTrigger
     */
    transform(swimlane: Swimlane, searchCountTrigger: number): number {
        if (searchCountTrigger === 0 || !swimlane?.grid?.length) {
            return 0;
        }
        let count: number = 0;
        swimlane.grid.forEach((widget: GridTile): void => {
            count += widget?.searchCount || 0;
        });
        return count;
    }
}
