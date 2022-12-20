import { Pipe, PipeTransform } from '@angular/core';
import { RestHelper } from '../../core-module/core.module';

@Pipe({
    name: 'formatDuration',
})
export class FormatDurationPipe implements PipeTransform {
    transform(value: string): string {
        return RestHelper.getDurationFormatted(value);
    }
}
