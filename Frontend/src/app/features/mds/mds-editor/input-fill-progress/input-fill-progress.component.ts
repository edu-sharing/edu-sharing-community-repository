import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RequiredMode } from '../../types/types';

@Component({
    selector: 'es-input-fill-progress',
    templateUrl: 'input-fill-progress.component.html',
    styleUrls: ['input-fill-progress.component.scss'],
})
export class InputFillProgressComponent {
    readonly RequiredMode = RequiredMode;

    @Input() current: FillTypeStatus;
    @Input() maximum: FillTypeStatus;
    @Output() showMissing = new EventEmitter();

    getStatus() {
        for (const type of Object.values(RequiredMode)) {
            if ((this.current as any)[type] !== (this.maximum as any)[type]) {
                return type;
            }
        }
        return 'finished';
    }

    getFullProgress() {
        let sum = this.current[RequiredMode.Mandatory];
        if (this.current[RequiredMode.Mandatory] === this.maximum[RequiredMode.Mandatory]) {
            sum += this.current[RequiredMode.MandatoryForPublish] || 0;
            if (
                this.current[RequiredMode.MandatoryForPublish] ===
                this.maximum[RequiredMode.MandatoryForPublish]
            ) {
                sum += this.current.optional || 0;
            }
        }
        return sum;
    }
    getPrimaryProgress() {
        return (this.current as any)[this.getStatus()] / (this.maximum as any)[this.getStatus()];
    }

    getCurrentMaximum() {
        if (this.getStatus() === RequiredMode.Mandatory) {
            return this.current[RequiredMode.Mandatory];
        }
        return (
            this.maximum[RequiredMode.Mandatory] +
            (this.maximum[RequiredMode.MandatoryForPublish] || 0)
        );
    }
    getSum(what: 'current' | 'maximum') {
        let sum = 0;
        for (const type of Object.values(RequiredMode)) {
            sum += (this as any)[what][type] || 0;
        }
        return sum;
    }
}
export type FillTypeStatus = {
    [key in RequiredMode]: number;
};
