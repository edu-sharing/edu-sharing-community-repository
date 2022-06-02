import {
    ApplicationRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { NodeEntriesService } from '../../../core-ui-module/node-entries.service';
import { Node } from '../../../core-module/rest/data-object';
import { NodeHelperService } from '../../../core-ui-module/node-helper.service';
import { ColorHelper, PreferredColor } from '../../../core-module/ui/color-helper';
import { OptionItem, Target } from '../../../core-ui-module/option-item';
import { DropdownComponent } from '../../../shared/components/dropdown/dropdown.component';
import { MatMenuTrigger } from '@angular/material/menu';
import { Toast } from 'src/app/core-ui-module/toast';
import { ConfigurationService } from '../../../core-module/rest/services/configuration.service';
import { RestConnectorService } from '../../../core-module/rest/services/rest-connector.service';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { ClickSource, InteractionType } from '../entries-model';

@Component({
    selector: 'es-node-entries-card',
    templateUrl: 'node-entries-card.component.html',
    styleUrls: ['node-entries-card.component.scss'],
})
export class NodeEntriesCardComponent<T extends Node> implements OnChanges, OnInit {
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    readonly ClickSource = ClickSource;
    @Input() dropdown: DropdownComponent;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    @Input() node: T;
    dropdownLeft: number;
    dropdownTop: number;
    showRatings: boolean;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        public applicationRef: ApplicationRef,
        public connector: RestConnectorService,
        public configService: ConfigurationService,
        private toast: Toast,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {}

    getTextColor() {
        return ColorHelper.getPreferredColor(this.node.collection.color) === PreferredColor.Black
            ? '#000'
            : '#fff';
    }
    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback(this.node))) {
            return always;
        }
        // we do NOT show any additional actions
        return [];
        // return options.filter((o) => o.showAsAction && o.showCallback(this.node)).slice(0, 3);
    }

    openContextmenu(event: MouseEvent | Event) {
        event.stopPropagation();
        event.preventDefault();
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        } else {
            ({ x: this.dropdownLeft, y: this.dropdownTop } = (
                event.target as HTMLElement
            ).getBoundingClientRect());
        }
        if (!this.entriesService.selection.selected.includes(this.node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(this.node);
        }
        // Wait for the menu to reflect changed options.
        setTimeout(() => {
            if (this.dropdown.canShowDropdown()) {
                this.menuTrigger.openMenu();
            } else {
                this.toast.toast('NO_AVAILABLE_OPTIONS');
            }
        });
    }

    getVisibleColumns() {
        return this.entriesService.columns.filter((c) => c.visible);
    }

    async openMenu(node: T) {
        this.entriesService.selection.clear();
        this.entriesService.selection.select(node);
        await this.applicationRef.tick();
        this.dropdown.menu.focusFirstItem();
    }

    async ngOnInit() {
        this.showRatings =
            (await this.configService.get('', 'none').toPromise()) !== 'none' &&
            (await this.connector
                .hasToolPermission(RestConstants.TOOLPERMISSION_RATE_READ)
                .toPromise());
    }
}
