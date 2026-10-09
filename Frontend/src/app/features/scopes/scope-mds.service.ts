import { Injectable, inject } from '@angular/core';
import { DEFAULT, HOME_REPOSITORY, MdsService, MdsWidget } from 'ngx-edu-sharing-api';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export type ScopeWidgets = Record<string, MdsWidget>;

/**
 * Widget definitions (captions, valuespaces) of the scope properties, taken from the scope mds
 * (`ccm:scope_mds`) or the default mds.
 */
@Injectable({
    providedIn: 'root',
})
export class ScopeMdsService {
    private mdsService = inject(MdsService);

    /** widgets of the given mds by property id (cached by `MdsService`) */
    observeWidgets(mds?: string | null): Observable<ScopeWidgets> {
        return this.mdsService
            .getMetadataSet({ repository: HOME_REPOSITORY, metadataSet: mds || DEFAULT })
            .pipe(
                map((definition) => {
                    const widgets: ScopeWidgets = {};
                    // the first definition of a widget wins (later ones are template specific)
                    for (const widget of definition.widgets ?? []) {
                        if (widget.id && !widgets[widget.id]) {
                            widgets[widget.id] = widget;
                        }
                    }
                    return widgets;
                }),
                catchError(() => of({} as ScopeWidgets)),
            );
    }

    static getLabel(widgets: ScopeWidgets | null | undefined, property: string): string {
        return widgets?.[property]?.caption || property;
    }

    static getValueLabel(
        widgets: ScopeWidgets | null | undefined,
        property: string,
        value: string,
    ): string {
        return widgets?.[property]?.values?.find((v) => v.id === value)?.caption || value;
    }

    static isMultiple(widget: MdsWidget | undefined): boolean {
        return !!widget?.type?.startsWith('multivalue');
    }
}
