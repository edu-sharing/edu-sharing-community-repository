import { NgModule } from '@angular/core';
import {BridgeService} from "./bridge.service";

@NgModule({
    providers:[
        BridgeService
    ],
})
export class CoreBridgeModule { }

export * from "./bridge.service"