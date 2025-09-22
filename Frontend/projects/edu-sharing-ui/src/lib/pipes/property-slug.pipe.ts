import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'propertySlug',
    standalone: false,
})
export class PropertySlugPipe implements PipeTransform {
    transform(value: string): string {
        return value?.replace(/[:.]/g, '_');
    }
}
