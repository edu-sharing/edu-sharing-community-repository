import { PipeTransform, Pipe } from '@angular/core';

@Pipe({ name: 'esUrl' })
export class UrlPipe implements PipeTransform {
    transform(value: any, args: any): any {
        if (!value.startsWith('http://') && !value.startsWith('https://')) {
            value = 'https://' + value;
        }
        let url = new URL(value);
        if (args['mode'] == 'domain') {
            return url.host;
        } else {
            return 'unknown or unspecified mode: ' + args['mode'];
        }
    }
}
