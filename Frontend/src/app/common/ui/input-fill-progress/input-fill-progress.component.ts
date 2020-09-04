/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {ConfigurationHelper, ConfigurationService} from '../../../core-module/core.module';

@Component({
  selector: 'app-input-fill-progress',
  templateUrl: 'input-fill-progress.component.html',
  styleUrls: ['input-fill-progress.component.scss']
})
export class InputFillProgressComponent {

    @Input() current: FillTypeStatus;
    @Input() maximum: FillTypeStatus;


    /**
    ngOnChanges(changes: SimpleChanges): void {
        if (this.minimum && this.maximum) {
        }
    }*/
    getStatus() {
        for(const type of Object.keys(FillType)) {
            if((this.current as any)[type] !== (this.maximum as any)[type]){
                return type;
            }
        }
        return null;
    }

    getFullProgress() {
        return ((this.current.required || 0) + (this.current.requiredPublish || 0)) / ((this.maximum.required || 0) + (this.maximum.requiredPublish || 0))
    }
    getPrimaryProgress() {
        return (this.current as any)[this.getStatus()] / (this.maximum as any)[this.getStatus()];
    }

    getCurrentMaximum() {
        if(this.getStatus() === FillType.required) {
            return this.current.required;
        }
        return this.maximum.required + (this.maximum.requiredPublish || 0);
    }
}
export type FillTypeStatus = {
    [key in FillType]: number;
};

export enum FillType {
    required = 'required',
    requiredPublish = 'requiredPublish',
    //recommended = 'recommended'
}
