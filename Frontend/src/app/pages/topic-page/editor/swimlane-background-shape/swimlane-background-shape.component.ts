import { Component, input, InputSignal } from '@angular/core';
import { SharedModule } from '../../../../shared/shared.module';
import { SwimlaneBackgroundShape } from '../../shared/types/swimlane-background-shape';

@Component({
    imports: [SharedModule],
    selector: 'es-swimlane-background-shape',
    templateUrl: './swimlane-background-shape.component.html',
    styleUrls: ['./swimlane-background-shape.component.scss'],
})
export class SwimlaneBackgroundShapeComponent {
    shape: InputSignal<SwimlaneBackgroundShape> = input(null);

    protected readonly SwimlaneBackgroundShape = SwimlaneBackgroundShape;
}
