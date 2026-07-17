import { inject, Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { ListItem } from '../types/list-item';
import { TranslationsService } from '../translations/translations.service';
import { delay, startWith, switchMap } from 'rxjs/operators';

@Pipe({
    name: 'esListItemLabel',
    standalone: false,
})
export class ListItemLabelPipe implements PipeTransform {
    private translate = inject(TranslateService);
    private translations = inject(TranslationsService);

    transform(item: ListItem, args = { fallback: item.name }): Observable<string> {
        const mapping = {
            NODE: 'NODE',
            COLLECTION: 'NODE',
            NODE_PROPOSAL: 'NODE_PROPOSAL',
            ORG: 'ORG',
            GROUP: 'GROUP',
            USER: 'USER',
            EVENT: 'EVENT',
            ASSIGNMENT: 'ASSIGNMENT',
            SUBMISSION: 'SUBMISSION',
            SHARE: 'SHARE',
            SUGGESTION: 'SUGGESTIONS',
        };
        if (item.label) {
            return rxjs.of(item.label);
        } else {
            return this.translations.waitForInit().pipe(
                startWith(undefined as void),
                delay(1),
                switchMap(() =>
                    this.translate.get(mapping[item.type] + '.' + item.name, {
                        fallback: args.fallback,
                    }),
                ),
            );
        }
    }
}
