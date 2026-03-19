import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
} from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionItem } from '../../types/option-item';
import * as rxjs from 'rxjs';
import { NodeEntriesService } from '../../services/node-entries.service';
import { startWith } from 'rxjs/operators';
import { NodeEntriesDataType } from '../data-type';
// TODO: Decide if providing focus highlights and ripples with this component is a good idea. When
// using `app-node-url` for cards, we might need highlights and ripples for the whole card while
// `app-node-url` should only wrap the title since links with lots of content confuse screen
// readers.

@Component({
    selector: 'es-option-button',
    template: `
        <button
            mat-icon-button
            color="primary"
            matTooltip="{{ option.name | translate }}"
            [class.display-none]="!isShown"
            [disabled]="!isEnabled"
            (click)="click(option, node)"
            attr.data-test="option-button-{{ option.name }}"
        >
            <i esIcon="{{ option.icon }}" [aria]="false"></i>
        </button>
    `,
    standalone: false,
})
export class OptionButtonComponent<T extends NodeEntriesDataType>
    implements OnChanges, AfterViewInit
{
    @Input() option: OptionItem;
    @Input() node: Node;

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        private entriesService: NodeEntriesService<T>,
    ) {}
    isShown = false;
    isEnabled = false;
    async ngOnChanges(changes: SimpleChanges) {
        this.isEnabled = await this.optionIsValid(this.option, this.node);
        this.isShown = await this.optionIsShown(this.option, this.node);
    }

    ngAfterViewInit(): void {
        rxjs.combineLatest([
            this.entriesService.options$.pipe(startWith(void 0 as void)),
        ]).subscribe(() => {
            void this.ngOnChanges(null);
        });
    }
    async optionIsValid(optionItem: OptionItem, node: Node): Promise<boolean> {
        if (optionItem.enabledCallback) {
            return await optionItem.enabledCallback([node]);
        }
        return optionItem.isEnabled;
    }

    private async optionIsShown(optionItem: OptionItem, node: Node): Promise<boolean> {
        if (optionItem.showCallback) {
            return optionItem.showCallback([node]);
        }
        return true;
    }

    async click(option: OptionItem, node: Node) {
        if (await this.optionIsShown(option, node)) {
            option.callback(node);
        }
    }
}
