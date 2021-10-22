import { Component, Input, OnInit } from '@angular/core';
import { FacetValue } from 'edu-sharing-api';
import { combineLatest, Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';

@Component({
    selector: 'app-mds-editor-widget-suggestion-chips',
    templateUrl: './mds-editor-widget-suggestion-chips.component.html',
    styleUrls: ['./mds-editor-widget-suggestion-chips.component.scss'],
})
export class MdsEditorWidgetSuggestionChipsComponent implements OnInit {
    @Input() widget: Widget;

    filteredSuggestions$: Observable<FacetValue[]>;

    /** The widget controlling the property that this widget is displaying suggestions for. */
    private primaryWidget: Widget;

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.primaryWidget = this.mdsEditorInstance.getPrimaryWidget(this.widget.definition.id);
        if (this.primaryWidget) {
            this.filteredSuggestions$ = this.getFilteredSuggestions(this.primaryWidget);
        } else {
            console.error(
                'Could not find corresponding primary widget for suggestion widget.\n',
                `widget: ${this.widget.definition.id}\n`,
                `group: ${this.mdsEditorInstance.groupId}\n`,
                `mds: ${this.mdsEditorInstance.mdsId}`,
            );
        }
    }

    add(suggestion: FacetValue): void {
        this.primaryWidget.addValue.emit({ id: suggestion.value, caption: suggestion.label });
    }

    /**
     * Returns an observable of suggestions, excluding the ones already included in the primary
     * widget's value.
     */
    private getFilteredSuggestions(primaryWidget: Widget): Observable<FacetValue[]> {
        // Suggestions, relevant for this widget.
        const widgetSuggestions$ = this.mdsEditorInstance.facets$.pipe(
            map((suggestions) => suggestions[this.widget.definition.id]),
            map((suggestions) => suggestions?.values.slice(0, 5)),
        );
        // Filter `widgetSuggestions$` by primary widget's value.
        return combineLatest([
            widgetSuggestions$,
            primaryWidget.observeValue().pipe(startWith(null as string[])),
        ]).pipe(
            map(([suggestions, values]) =>
                suggestions?.filter(
                    (suggestion) => !values?.some((value) => value === suggestion.value),
                ),
            ),
        );
    }
}
