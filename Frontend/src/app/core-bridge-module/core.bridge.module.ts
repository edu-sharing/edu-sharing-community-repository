import { NgModule } from '@angular/core';
import {BridgeService} from "./bridge.service";

@NgModule({
    providers:[
        BridgeService
    ],
    entryComponents:[],
})
export class CoreBridgeModule { }

export * from "./bridge.service"