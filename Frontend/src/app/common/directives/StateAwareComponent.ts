import {AfterViewInit, OnDestroy} from '@angular/core';
import {TemporaryStorageService} from "../../core-module/core.module";

/**
 * Helper class for components that may store and revert state after navigating
 * Please note that this is only allowed for singleton instances at the moment
 */
export class StateAwareComponent implements AfterViewInit,OnDestroy{
    private stateRecovered=false;

    /**
     * Call the super constructor with your injected service +
     * add a list of all field names you want to store + recover
     * @param {TemporaryStorageService} temporaryStorage
     * @param {string[]} storedFields
     */
    constructor(
        protected temporaryStorage : TemporaryStorageService,
        protected storedFields : string[] = null
    ){

    }
    private getIdForKey(key:string){
        return this.constructor.name + "_" + key;
    }
    public ngOnDestroy(){
        for(let key of this.getStoredFields()){
            this.temporaryStorage.set(this.getIdForKey(key), (this as any)[key]);
        }
    }
    public ngAfterViewInit(){
        setTimeout(()=> {
            for (let key of this.getStoredFields()) {
                if (this.temporaryStorage.get(this.getIdForKey(key)) != null) {
                    (this as any)[key] = this.temporaryStorage.pop(this.getIdForKey(key), (this as any)[key]);
                    this.stateRecovered = true;
                }
            }
            setTimeout(()=>this.onInitDone());
        });
    }
    public onInitDone(){
    }
    public isStateRecovered(){
        return this.stateRecovered;
    }

    private getStoredFields() {
        return this.storedFields && this.storedFields.length ? this.storedFields : Object.keys(this);
    }
}
