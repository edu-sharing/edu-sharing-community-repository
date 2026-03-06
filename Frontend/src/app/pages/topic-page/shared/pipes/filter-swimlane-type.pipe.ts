import { Pipe, PipeTransform } from '@angular/core';
import { Swimlane } from '../types/swimlane';

@Pipe({
    name: 'filterSwimlaneType',
    standalone: true,
})
export class FilterSwimlaneTypePipe implements PipeTransform {
    /**
     * Filters a given array of swimlanes by a given type.
     *
     * @param swimlanes
     * @param type
     * @param trigger
     */
    transform(swimlanes: Swimlane[], type: string, trigger: number): Swimlane[] {
        // trigger is used to prevent usage of pure: false
        if (!swimlanes?.length || !trigger) {
            return [];
        }
        return swimlanes.filter((swimlane: Swimlane): boolean => swimlane.type === type);
    }
}
