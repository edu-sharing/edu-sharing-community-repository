import { ComponentFactoryResolver } from '@angular/core';
import { CUSTOM_COMPONENTS } from '../custom-module/custom.module';

export class CustomHelper {
    static getCustomComponents(
        componentName: string,
        componentFactoryResolver: ComponentFactoryResolver,
    ) {
        let result = [];
        for (let c of CUSTOM_COMPONENTS) {
            if (c.targetComponent == componentName) {
                c.factory = componentFactoryResolver.resolveComponentFactory(c.component);
                result.push(c);
            }
        }
        return result;
    }
}
