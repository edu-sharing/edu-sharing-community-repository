import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    input,
    Input,
    InputSignal,
    Output,
    ViewEncapsulation,
} from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-side-menu-wrapper',
    encapsulation: ViewEncapsulation.Emulated,
    imports: [CommonModule, EduSharingUiCommonModule, MatIconButton, TranslateModule],
    templateUrl: './side-menu-wrapper.component.html',
    styleUrls: ['./side-menu-wrapper.component.scss'],
})
export class SideMenuWrapperComponent {
    closeDisabled: InputSignal<boolean> = input<boolean>(false);
    mobileHidden: InputSignal<boolean> = input<boolean>(false);

    @Input() position: string = 'right';
    @Input() selectedMenuItem: string = '';
    @Output() closeContentView: EventEmitter<boolean> = new EventEmitter<boolean>();

    /**
     * Emits that the content view should be closed.
     */
    closeContent(): void {
        this.closeContentView.emit(true);
    }
}
