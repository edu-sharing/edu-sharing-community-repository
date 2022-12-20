import { Pipe, PipeTransform } from '@angular/core';
import { DurationFormat, RestHelper } from '../../core-module/core.module';

@Pipe({
    name: 'formatDuration',
})
export class FormatDurationPipe implements PipeTransform {
    transform(
        value: string,
        args = {
            format: DurationFormat.Hms,
        },
    ): string {
        return RestHelper.getDurationFormatted(value, args.format);
    }
}
