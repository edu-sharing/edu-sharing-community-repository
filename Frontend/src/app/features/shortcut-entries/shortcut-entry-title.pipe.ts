import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { NodeTitlePipe } from 'ngx-edu-sharing-ui';
import { Observable, of, timer } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ExtendedShortcutEntry } from './shortcut-entries.component';

@Pipe({
    name: 'shortcutEntryTitle',
    standalone: true,
})
export class ShortcutEntryTitlePipe implements PipeTransform {
    private translate = inject(TranslateService);
    private nodeTitlePipe = new NodeTitlePipe(this.translate);

    /**
     * Transforms a given extended shortcut entry into its individual title.
     *
     * @param item
     * @param index
     * @param i18nPrefix
     */
    transform(item: ExtendedShortcutEntry, index: number, i18nPrefix: string): Observable<string> {
        if (item?.title) {
            return of(item.title);
        }
        if ('node' in item && item.node !== null) {
            return of(this.nodeTitlePipe.transform(item.node));
        }
        if ('id' in item && item.id !== null) {
            const key = i18nPrefix + item.id;
            return this.translate.get(key, {
                fallback: timer(500).pipe(
                    switchMap(() => this.translate.get(key)),
                    map((secondResult) =>
                        secondResult !== key ? secondResult : `[MISSING_TRANSLATION: ${key}]`,
                    ),
                ),
            });
        }
        return of(`Unnamed #${index + 1}`);
    }
}
