import { Pipe, PipeTransform } from '@angular/core';
import { Swimlane } from '../types/swimlane';

@Pipe({
    name: 'filterVisibleSwimlanes',
    standalone: true,
})
export class FilterVisibleSwimlanePipe implements PipeTransform {
    transform(
        swimlanes: Swimlane[] | null,
        searchInput: string | null,
        hitMatching: Map<string, boolean> | null,
    ): Swimlane[] {
        if (!swimlanes?.length) {
            return [];
        }

        if (!searchInput) {
            return swimlanes;
        }

        if (!hitMatching) {
            return [];
        }

        return swimlanes.filter((swimlane) => hitMatching.get(swimlane.id));
    }
}
