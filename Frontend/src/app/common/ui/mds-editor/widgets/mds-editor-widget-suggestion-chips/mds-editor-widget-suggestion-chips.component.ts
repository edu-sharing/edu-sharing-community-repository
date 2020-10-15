import { Component, Input, OnInit } from '@angular/core';
import { combineLatest, Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { MdsWidgetValue } from '../../types';

@Component({
    selector: 'app-mds-editor-widget-suggestion-chips',
    templateUrl: './mds-editor-widget-suggestion-chips.component.html',
    styleUrls: ['./mds-editor-widget-suggestion-chips.component.scss'],
})
export class MdsEditorWidgetSuggestionChipsComponent implements OnInit {
    @Input() widget: Widget;

    filteredSuggestions$: Observable<MdsWidgetValue[]>;

    /** The widget controlling the property that this widget is displaying suggestions for. */
    private primaryWidget: Widget;

    constructor(private mdsEditorInstance: MdsEditorInstanceService) {}

    ngOnInit(): void {
        this.primaryWidget = this.mdsEditorInstance.getPrimaryWidget(this.widget.definition.id);
        this.filteredSuggestions$ = this.getFilteredSuggestions(this.primaryWidget);
    }

    add(suggestion: MdsWidgetValue): void {
        this.primaryWidget.addValue.emit(suggestion);
    }

    /**
     * Returns an observable of suggestions, excluding the ones already included in the primary
     * widget's value.
     */
    private getFilteredSuggestions(primaryWidget: Widget): Observable<MdsWidgetValue[]> {
        // Suggestions, relevant for this widget.
        const widgetSuggestions$ = this.mdsEditorInstance.suggestions$.pipe(
            map((suggestions) => suggestions[this.widget.definition.id]),
        );
        // Filter `widgetSuggestions$` by primary widget's value.
        return combineLatest([
            widgetSuggestions$,
            primaryWidget.observeValue().pipe(startWith(null as string[])),
        ]).pipe(
            map(([suggestions, values]) =>
                suggestions?.filter(
                    (suggestion) => !values?.some((value) => value === suggestion.id),
                ),
            ),
        );
    }
}
