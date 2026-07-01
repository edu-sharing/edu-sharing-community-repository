import { Injectable, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';
import { first } from 'rxjs/operators';
import { TranslationsService } from './translations.service';

/**
 * Central, translated {@link MatPaginatorIntl} for every `<mat-paginator>` in the app.
 * Registered as a global `MatPaginatorIntl` provider so paginators are translated without any
 * per-component `_intl` wiring. Labels are (re)read from the `PAGINATOR.*` i18n keys once the
 * translations are initialized and again on every language change.
 */
@Injectable()
export class TranslatedMatPaginatorIntl extends MatPaginatorIntl {
    private translate = inject(TranslateService);
    private translations = inject(TranslationsService);

    constructor() {
        super();
        this.translations
            .waitForInit()
            .pipe(first())
            .subscribe(() => this.updateLabels());
        this.translate.onLangChange.subscribe(() => this.updateLabels());
    }

    private updateLabels(): void {
        this.itemsPerPageLabel = this.translate.instant('PAGINATOR.itemsPerPageLabel');
        this.firstPageLabel = this.translate.instant('PAGINATOR.firstPageLabel');
        this.lastPageLabel = this.translate.instant('PAGINATOR.lastPageLabel');
        this.nextPageLabel = this.translate.instant('PAGINATOR.nextPageLabel');
        this.previousPageLabel = this.translate.instant('PAGINATOR.previousPageLabel');
        this.getRangeLabel = (page, pageSize, length) =>
            this.translate.instant('PAGINATOR.getRangeLabel', {
                page: page + 1,
                pageSize,
                length,
                pageCount: Math.ceil(length / pageSize),
            });
        this.changes.next();
    }
}
