import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { JumpMark } from '../../../../../services/jump-marks.service';

/**
 * Jump-marks navigation menu.
 */
@Component({
    selector: 'es-card-jump-marks',
    templateUrl: './card-jump-marks.component.html',
    styleUrls: ['./card-jump-marks.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CardJumpMarksComponent {
    @Input() jumpMarks: JumpMark[];
    @Input() activeJumpMark: JumpMark;

    @Output() scrollToJumpMark = new EventEmitter<JumpMark>();

    constructor() {}
}
