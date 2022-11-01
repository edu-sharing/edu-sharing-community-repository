import { InjectionToken, TemplateRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { RestHelper } from '../../../core-module/core.module';
import { DialogButton } from '../../../core-module/ui/dialog-button';
import { CardAvatar } from './card-dialog-container/card-header/card-avatar';

export const CARD_DIALOG_DATA = new InjectionToken<any>('CardDialogData');

export class CardDialogConfig<D = unknown> {
    title?: string;
    subtitle?: string;
    avatar?: CardAvatar;
    buttons?: DialogButton[];
    customBottomBarContent?: TemplateRef<unknown>;
    contentPadding?: number = 25;
    width?: number;
    minWidth?: number | string;
    maxWidth?: number | string = '95%';
    height?: number;
    minHeight?: number | string;
    maxHeight?: number | string = '95%';
    closable?: Closable = Closable.Casual;
    autoFocus?: AutoFocusTarget | string | boolean = 'first-tabbable';
    data?: D;
}

/**
 * Standard ways for the user to close the dialog, ordered by increasing resistance.
 */
export enum Closable {
    /**
     * The dialog can be closed by pressing Escape, clicking the 'X' button or clicking the
     * backdrop.
     */
    Casual,
    /**
     * The dialog can be closed by pressing Escape or clicking the 'X' button.
     */
    Standard,
    /**
     * The dialog can be closed by pressing Escape or clicking the 'X' button after confirming a
     * dialog.
     */
    Confirm,
    /**
     * The dialog cannot be closed via standard actions.
     */
    Disabled,
}

export type AutoFocusTarget = 'dialog' | 'first-tabbable' | 'first-heading';

export function configForNode(node: Node): Partial<CardDialogConfig> {
    return {
        avatar: { kind: 'image', url: node.iconURL },
        subtitle: RestHelper.getTitle(node),
    };
}

export function configForNodes(
    nodes: Node[],
    translate: TranslateService,
): Observable<Partial<CardDialogConfig>> {
    if (nodes.length === 1) {
        return of(configForNode(nodes[0]));
    }
    return translate.get('CARD_SUBTITLE_MULTIPLE', { count: nodes.length }).pipe(
        map((subtitle) => ({
            avatar: null,
            subtitle,
        })),
    );
}
