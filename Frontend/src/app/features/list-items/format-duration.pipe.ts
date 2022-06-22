import { Pipe, PipeTransform } from '@angular/core';
import { Node, RestHelper } from '../../core-module/core.module';

@Pipe({
    name: 'formatDuration',
})
export class FormatDurationPipe implements PipeTransform {
    transform(node: Node): string {
        // TODO: Take the actual field instead of `Node`.
        //
        // Is `cclom:duration` always given in seconds or do we have to handle other formats?
        return RestHelper.getDurationFormatted(node);
    }
}
