import { Pipe, PipeTransform } from '@angular/core';
import { Swimlane } from '../types/swimlane';

@Pipe({
    name: 'filterVisibleSwimlanes',
    standalone: true,
})
export class FilterVisibleSwimlanePipe implements PipeTransform {
    transform(
        swimlanes: Swimlane[] | null,
        searchFiltered: boolean = false,
        hitMatching: Map<string, boolean> | null,
    ): Swimlane[] {
        if (!swimlanes?.length) {
            return [];
        }

        if (!searchFiltered) {
            return swimlanes;
        }

        if (!hitMatching) {
            return [];
        }

        return swimlanes.filter((swimlane) => hitMatching.get(swimlane.id));
    }
}
