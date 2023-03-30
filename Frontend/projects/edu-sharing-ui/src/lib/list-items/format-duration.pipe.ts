import { Pipe, PipeTransform } from '@angular/core';
import { DurationFormat } from '../util/duration-helper';
import { DurationHelper } from '../util/duration-helper';

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
        return DurationHelper.getDurationFormatted(value, args.format);
    }
}
