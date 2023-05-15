import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DateHelper, FormatOptions } from '../../core-ui-module/DateHelper';

@Pipe({ name: 'formatDate' })
export class FormatDatePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}

    transform(
        value: Date | number | string,
        args?: { time?: boolean; date?: boolean; relative?: boolean },
    ): string;
    transform(
        value: Date | number | string,
        args: { time?: boolean; date?: boolean; relative?: boolean; async: true },
    ): Observable<string>;
    transform(
        value: Date | number | string,
        args: { time?: boolean; date?: boolean; relative?: boolean; async?: boolean } = null,
    ): string | Observable<string> {
        if (!value) {
            return args?.async ? rxjs.of('') : '';
        }
        let options = new FormatOptions();
        if (args && args.time != null) options.showAlwaysTime = args.time;
        if (args && args.date != null) options.showDate = args.date;
        if (args && args.relative !== null) options.useRelativeLabels = args.relative;
        if (args?.async) {
            return this.translate
                .get('dummy') // Wait for the translation service to be ready
                .pipe(map(() => DateHelper.formatDate(this.translate, value, options)));
        } else {
            return DateHelper.formatDate(this.translate, value, options);
        }
    }
}
