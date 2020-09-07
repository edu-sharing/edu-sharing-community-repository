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
    
    getStatus() {
        for(const type of Object.keys(FillType)) {
            if((this.current as any)[type] !== (this.maximum as any)[type]){
                return type;
            }
        }
        return null;
    }

    getFullProgress() {
        let sum=this.current.required;
        if(this.current.required === this.maximum.required) {
            sum += (this.current.requiredPublish || 0)
            if(this.current.requiredPublish === this.maximum.requiredPublish) {
                sum += (this.current.optional || 0)
            }
        }
        return sum;
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
    getSum(what: 'current' | 'maximum') {
        let sum = 0;
        for(const type of Object.keys(FillType)) {
            sum += (this as any)[what][type] || 0;
        }
        return sum;
    }
}
export type FillTypeStatus = {
    [key in FillType]: number;
};

export enum FillType {
    required = 'required',
    requiredPublish = 'requiredPublish',
    //recommended = 'recommended'
    optional = 'optional'
}
