import { PipeTransform, Pipe } from '@angular/core';

@Pipe({ name: 'url' })
export class UrlPipe implements PipeTransform {
    transform(value: any, args: any): any {
        let url = new URL(value);
        if (args['mode'] == 'domain') {
            return url.host;
        } else {
            return 'unknown or unspecified mode: ' + args['mode'];
        }
    }
}
