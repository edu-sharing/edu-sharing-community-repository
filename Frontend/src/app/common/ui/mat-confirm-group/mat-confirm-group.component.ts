import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, NgZone, OnInit, Output, ViewChild } from '@angular/core';
import { ConfigurationService, DialogButton } from '../../../core-module/core.module';
import { UIAnimation } from '../../../../../projects/edu-sharing-ui/src/lib/util/ui-animation';
import { CordovaService } from '../../services/cordova.service';
import { ThemePalette } from '@angular/material/core';
import { MatMenu, MatMenuTrigger } from '@angular/material/menu';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'es-mat-confirm-group',
    templateUrl: 'mat-confirm-group.component.html',
    styleUrls: ['mat-confirm-group.component.scss'],
})
export class MatConfirmGroupComponent implements OnInit {
    @ViewChild('menu') menu: MatMenu;
    @Input() label: string;
    @Input() description: string;
    @Input() parent: MatButton;
    @Input() icon: string;
    @Output() click = new EventEmitter<MouseEvent>();
    constructor(private ngZone: NgZone) {}

    ngOnInit(): void {}

    getParentWidth() {
        return this.parent?._elementRef?.nativeElement.getBoundingClientRect().width;
    }
}

export interface ConfirmAction {
    icon?: string;
    label?: string;
}
