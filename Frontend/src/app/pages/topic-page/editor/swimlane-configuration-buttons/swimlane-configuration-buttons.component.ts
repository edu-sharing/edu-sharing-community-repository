import {
    Component,
    computed,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    Signal,
    signal,
    WritableSignal,
    inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconButton } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import { SwimlaneBackgroundShape } from '../../shared/types/swimlane-background-shape';
import {
    DEFAULT_BG_COLOR,
    SWIMLANE_BACKGROUND_SHAPE_OPTIONS,
} from '../../shared/types/custom-definitions';
import { ColorPickerComponent } from '../../widgets/shared/color-picker/color-picker.component';
import { SwimlaneBackgroundShapeComponent } from '../swimlane-background-shape/swimlane-background-shape.component';

@Component({
    selector: 'es-swimlane-configuration-buttons',
    imports: [
        EduSharingUiCommonModule,
        ColorPickerComponent,
        FormsModule,
        MatIconButton,
        MatMenuModule,
        SwimlaneBackgroundShapeComponent,
        TranslateModule,
    ],
    templateUrl: './swimlane-configuration-buttons.component.html',
    styleUrls: ['./swimlane-configuration-buttons.component.scss'],
})
export class SwimlaneConfigurationButtonsComponent {
    private topicPageHelperService = inject(TopicPageHelperService);

    @Input() pageVariantNode: Node;
    @Input() swimlaneColor: string = DEFAULT_BG_COLOR;
    @Input() swimlaneIndex: number = -1;
    @Input() swimlaneShape: SwimlaneBackgroundShape = SwimlaneBackgroundShape.None;
    updateInProgress: InputSignal<boolean> = input(false);
    @Output() swimlaneShapeUpdated: EventEmitter<SwimlaneBackgroundShape> =
        new EventEmitter<SwimlaneBackgroundShape>();
    @Output() swimlaneShapeMirroringToggled: EventEmitter<void> = new EventEmitter<void>();
    colorChangeInProgress: WritableSignal<boolean> = signal(false);
    inputsDisabled: Signal<boolean> = computed(() => {
        return this.updateInProgress() || this.colorChangeInProgress();
    });

    /**
     * The color was changed using the color picker.
     */
    changedColor(color: string): void {
        // attempt to persist the color change
        this.colorChangeInProgress.set(true);
        setTimeout(() => {
            this.colorChangeInProgress.set(false);
        }, 500);
        const colorChangeSuccessful: boolean = this.topicPageHelperService.persistColorChange(
            color,
            this.pageVariantNode,
            this.swimlaneIndex,
        );
        // update swimlane color
        if (colorChangeSuccessful) {
            this.swimlaneColor = color;
        }
    }

    /**
     * Handles the update of a swimlane shape by emitting the selected shape.
     *
     * @param backgroundShape
     */
    updateSwimlaneShape(backgroundShape: SwimlaneBackgroundShape): void {
        this.swimlaneShapeUpdated.emit(backgroundShape);
    }

    /**
     * Handles the toggling of the mirroring of the swimlane shape by emitting the toggle event.
     */
    toggleSwimlaneShapeMirroring(): void {
        this.swimlaneShapeMirroringToggled.emit();
    }

    protected readonly SwimlaneBackgroundShape = SwimlaneBackgroundShape;
    protected readonly swimlaneBackgroundShapeOptions = SWIMLANE_BACKGROUND_SHAPE_OPTIONS;
}
