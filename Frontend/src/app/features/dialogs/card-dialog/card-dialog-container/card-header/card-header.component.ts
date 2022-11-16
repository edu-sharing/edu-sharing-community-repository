import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CardAvatar } from './card-avatar';


@Component({
    selector: 'es-card-header',
    templateUrl: './card-header.component.html',
    styleUrls: ['./card-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardHeaderComponent {
    /** DOM-ID suffix for elements of this dialog. */
    @Input() id: number;
    @Input() title: string;
    @Input() subtitle?: string;
    @Input() avatar?: CardAvatar;
    @Input() showCloseButton: boolean = true;
    @Input() disableCloseButton: boolean = false;
    @Output() closeButtonClick: EventEmitter<void> = new EventEmitter();

    getIconImageUrl(): string {
        if (this.avatar?.kind === 'image') {
            return this.avatar.url;
        } else {
            return null;
        }
    }

    getIcon(): string {
        if (this.avatar?.kind === 'icon') {
            return this.avatar.icon;
        } else {
            return null;
        }
    }

    onClose(): void {
        this.closeButtonClick.emit();
    }
}
