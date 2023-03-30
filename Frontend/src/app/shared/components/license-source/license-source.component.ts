import { Component, Input, EventEmitter, Output, ViewChild, ElementRef } from '@angular/core';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';

@Component({
    selector: 'es-license-source',
    templateUrl: 'license-source.component.html',
    styleUrls: ['license-source.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('dialog', UIAnimation.switchDialog()),
    ],
})
export class LicenseSourceComponent {
    showCcAuthor: boolean;
    @Input() ccTitleOfWork = '';
    @Output() ccTitleOfWorkChange = new EventEmitter<string>();
    @Input() ccSourceUrl = '';
    @Output() ccSourceUrlChange = new EventEmitter<string>();
    @Input() ccProfileUrl = '';
    @Output() ccProfileUrlChange = new EventEmitter<string>();
}
