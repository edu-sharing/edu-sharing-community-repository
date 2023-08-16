import { Component, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'es-mat-link',
    templateUrl: 'link.component.html',
    styleUrls: ['link.component.scss'],
})
/**
 * A basic link that should be used whenever a button is not the best solution but rather a link is preferable
 * Will handle keyup.enter automatically for the click binding
 */
export class LinkComponent {
    @Output() click = new EventEmitter();
}
