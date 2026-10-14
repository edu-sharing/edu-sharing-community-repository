import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ListItem } from 'ngx-edu-sharing-ui';

/**
 * Selection of the properties for an export: either entered as free text (one per line / comma separated)
 * or picked from a list of available columns. The value is always kept as newline separated string.
 */
@Component({
    selector: 'es-admin-export-properties',
    templateUrl: 'export-properties.component.html',
    styleUrls: ['export-properties.component.scss'],
    standalone: false,
})
export class AdminExportPropertiesComponent {
    /** selected properties, newline separated */
    @Input() properties = '';
    @Output() propertiesChange = new EventEmitter<string>();
    /** columns which can be picked in the "list" mode */
    @Input() columns: ListItem[] = [];
    /** allow editing the properties as free text, otherwise only the list is shown */
    @Input() allowInput = true;
    @Input() label = 'ADMIN.STATISTICS.EXPORT_PROPERTIES';
    @Input() hint = 'ADMIN.STATISTICS.EXPORT_PROPERTIES_HINT';
    @Input() mode: 'input' | 'list' = 'input';

    /** currently selected property names (comma or newline separated) */
    private getNames(): string[] {
        return (this.properties ?? '')
            .split(/[\n,]/)
            .map((p) => p.trim())
            .filter(Boolean);
    }

    isSelected(column: ListItem): boolean {
        return this.getNames().includes(column.name);
    }

    toggle(column: ListItem, selected: boolean) {
        const names = this.getNames().filter((n) => n !== column.name);
        if (selected) {
            names.push(column.name);
        }
        this.setProperties(names.join('\n'));
    }

    setProperties(properties: string) {
        this.properties = properties;
        this.propertiesChange.emit(properties);
    }
}
