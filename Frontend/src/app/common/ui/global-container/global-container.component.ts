import { Component, ViewChild } from '@angular/core';

@Component({
    selector: 'es-global-container',
    templateUrl: 'global-container.component.html',
    styleUrls: ['global-container.component.scss'],
})
/**
 * Global components (always visible regardless of route
 */
export class GlobalContainerComponent {
    static instance: GlobalContainerComponent;

    @ViewChild('rocketchat') rocketchat: any; // using any to bypass Circular Dependency issues

    constructor() {
        GlobalContainerComponent.instance = this;
    }

}
