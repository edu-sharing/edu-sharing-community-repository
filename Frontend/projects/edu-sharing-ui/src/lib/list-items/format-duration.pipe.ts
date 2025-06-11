import { Pipe, PipeTransform } from '@angular/core';
import { DurationFormat, DurationHelper } from '../util/duration-helper';

@Pipe({
    name: 'formatDuration',
    standalone: false,
})
export class FormatDurationPipe implements PipeTransform {
    transform(
        value: string,
        args = {
            format: DurationFormat.Hms,
        },
    ): string {
        return DurationHelper.getDurationFormatted(value, args.format);
    }
}
