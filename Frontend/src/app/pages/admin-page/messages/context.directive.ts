import { Pipe, PipeTransform } from '@angular/core';
import { Context } from 'ngx-edu-sharing-api';

@Pipe({
    name: 'esContextName',
    standalone: true,
})
export class ContextNamePipe implements PipeTransform {
    transform(value: Context) {
        return value.domain?.[0] || value.id;
    }
}
