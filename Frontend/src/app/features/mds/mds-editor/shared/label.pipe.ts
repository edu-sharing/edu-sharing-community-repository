import { Pipe, PipeTransform } from '@angular/core';
import { Widget } from '../mds-editor-instance.service';

@Pipe({
    name: 'label',
})
export class LabelPipe implements PipeTransform {
    transform(value: string, widget: Widget): string {
        return (
            widget.definition.values?.find((widgetValue) => widgetValue.id === value)?.caption ??
            value
        );
    }
}
