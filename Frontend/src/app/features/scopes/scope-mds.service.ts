import { Injectable, inject } from '@angular/core';
import {
    ActiveScope,
    DEFAULT,
    HOME_REPOSITORY,
    MdsDefinition,
    MdsService,
    MdsValue,
    MdsWidget,
} from 'ngx-edu-sharing-api';
import { ColumnType, ListItem } from 'ngx-edu-sharing-ui';
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
    /** tabs (work sets) of the editorial page, see query ngsearch */
    static readonly VIEW_WIDGET = 'virtual:scope_view';
    /**
     * value of ccm:editorial_inventory for "no decision yet" (same id as the inbox view): never
     * stored, choosing it removes the value
     */
    static readonly INVENTORY_NONE = 'inbox';

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

    /** the value of the valuespace (caption, icon), undefined for free values */
    static getValue(
        widgets: ScopeWidgets | null | undefined,
        property: string,
        value: string,
    ): MdsValue | undefined {
        return widgets?.[property]?.values?.find((v) => v.id === value);
    }

    static getValueLabel(
        widgets: ScopeWidgets | null | undefined,
        property: string,
        value: string,
    ): string {
        return ScopeMdsService.getValue(widgets, property, value)?.caption || value;
    }

    /**
     * the card columns of the active scope (ScopePresentation.cardColumns) instead of the mds list.
     * Only `Default` is set, the table reuses it. Formats of the mds columns are kept. Without a
     * configuration the mds list stays
     */
    static applyCardColumns(
        defaults: ColumnType,
        scope: ActiveScope | null | undefined,
        mds?: MdsDefinition | null,
    ): ColumnType {
        const ids = scope?.definition?.presentation?.cardColumns;
        if (!ids?.length) {
            return defaults;
        }
        return { Default: ScopeMdsService.toColumns(ids, defaults?.Default ?? [], mds) };
    }

    /** the given properties as columns, existing columns (format, label) are reused */
    static toColumns(ids: string[], columns: ListItem[], mds?: MdsDefinition | null): ListItem[] {
        return ids.map(
            (id) =>
                columns.find((c) => c.type === 'NODE' && c.name === id) ??
                Object.assign(new ListItem('NODE', id), {
                    label: mds?.widgets?.find((w) => w.id === id)?.caption,
                }),
        );
    }

    static isMultiple(widget: MdsWidget | undefined): boolean {
        return !!widget?.type?.startsWith('multivalue');
    }
}
