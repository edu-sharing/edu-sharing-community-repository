import type { DropdownComponent } from '../dropdown/dropdown.component';
import type { ActionbarComponent } from '../actionbar/actionbar.component';
import type { ListEventInterface, NodeEntriesDisplayType } from '../node-entries/entries-model';
import { EventEmitter, inject, Injectable, NgZone, OnDestroy } from '@angular/core';
import { AuthenticationService, ME, NetworkService, Node, UserService } from 'ngx-edu-sharing-api';
import { take, takeUntil } from 'rxjs/operators';
import { CustomOptions, OptionItem, Scope, Target } from '../types/option-item';
import { OptionsHelperService } from './abstract/options-helper.service';
import { fromEvent, Subject, Subscription } from 'rxjs';
import { matchesShortcutCondition } from '../types/keyboard-shortcuts';
import { KeyboardShortcutsService } from './abstract/keyboard-shortcuts.service';
import { ActivatedRoute } from '@angular/router';
import { LocalEventsService } from './local-events.service';
import { Toast } from './abstract/toast.service';
import { GlobalStateService } from 'ngx-rendering-service-api';
import { NodeEntriesDataType } from '../node-entries/data-type';

type DeleteEvent = {
    objects: Node[] | any;
    count: number;
    error: boolean;
};
export interface OptionsHelperComponents {
    /** One or several actionbars sharing the same computed options. */
    actionbar?: ActionbarComponent | ActionbarComponent[];
    dropdown?: DropdownComponent;
    list?: ListEventInterface<NodeEntriesDataType>;
}

export interface OptionData {
    scope: Scope | string;
    activeObjects?: Node[] | any[];
    selectedObjects?: Node[] | any[];
    allObjects?: Node[] | any[];
    parent?: Node | any;
    customDownloadUrl?: string;
    customOptions?: CustomOptions;
    /**
     * custom interceptor to modify the default options array
     */
    postPrepareOptions?: (options: OptionItem[], objects: Node[]) => void;
}
@Injectable()
export class OptionsHelperDataService implements OnDestroy {
    private ngZone = inject(NgZone);
    private route = inject(ActivatedRoute, { optional: true });
    private localEvents = inject(LocalEventsService);
    private authenticationService = inject(AuthenticationService);
    private toast = inject(Toast);
    private userService = inject(UserService);
    private networkService = inject(NetworkService);
    private globalStateService = inject(GlobalStateService);
    private keyboardShortcutsService = inject(KeyboardShortcutsService, { optional: true });
    private optionsHelperService = inject(OptionsHelperService, { optional: true });

    private components: OptionsHelperComponents;
    private data: OptionData;
    private keyboardShortcutsSubscription: Subscription;
    private globalOptions: OptionItem[];
    private destroyed = new Subject<void>();

    readonly virtualNodesAdded = new EventEmitter<Node[]>();
    readonly nodesChanged = new EventEmitter<Node[] | void>();
    readonly nodesDeleted = new EventEmitter<DeleteEvent>();
    readonly displayTypeChanged = new EventEmitter<NodeEntriesDisplayType>();

    constructor() {
        this.registerStaticSubscriptions();
        this.globalStateService.downloadUrl$.pipe(takeUntil(this.destroyed)).subscribe(() => {
            if (this.data) {
                void this.refreshComponents();
            }
        });
    }

    /** Performs subscriptions that don't have to be refreshed. */
    private registerStaticSubscriptions(): void {
        this.localEvents.nodesDeleted
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => this.components?.list?.deleteNodes(nodes));
        this.localEvents.nodesChanged
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => this.components?.list?.updateNodes(nodes));
    }
    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    async initComponents(
        actionbar: ActionbarComponent | ActionbarComponent[] = null,
        list: ListEventInterface<NodeEntriesDataType> = null,
        dropdown: DropdownComponent = null,
    ) {
        this.components = {
            actionbar,
            list,
            dropdown,
        };
        if ((await this.userService.getUser(ME).pipe(take(1)).toPromise())?.person?.authorityName) {
            await this.networkService.getRepositories().pipe(take(1)).toPromise();
        }
    }

    getData() {
        if (!this.data) {
            return null;
        }
        this.data.customDownloadUrl = this.globalStateService.downloadUrl$.value;
        return this.data;
    }

    setData(data: OptionData) {
        this.data = this.optionsHelperService?.wrapOptionCallbacks(data) || data;
    }

    async refreshComponents() {
        if (this.data == null) {
            console.warn('options helper refresh called but no data previously bound');
            return;
        }
        if (this.optionsHelperService == null) {
            console.warn('optionsHelperService not provided. No default actions will be generated');
            return;
        }
        this.globalOptions = await this.getAvailableOptions(Target.Actionbar);
        await this.optionsHelperService?.refreshComponents(this.components, this.getData());
    }

    getAvailableOptions(target: Target, objects: Node[] = null) {
        return this.optionsHelperService?.getAvailableOptions(
            target,
            objects,
            this.components,
            this.getData(),
        );
    }

    private addVirtualObjects(objects: any[]) {
        objects = objects.map((o: any) => {
            o.virtual = true;
            return o;
        });
        this.virtualNodesAdded.emit(objects);
        if (this.components?.list) {
            this.components?.list.addVirtualNodes(objects);
        }
    }

    registerGlobalKeyboardShortcuts() {
        this.ngZone.runOutsideAngular(() => {
            if (!this.keyboardShortcutsSubscription) {
                this.keyboardShortcutsSubscription = fromEvent(document, 'keydown')
                    .pipe(takeUntil(this.destroyed))
                    .subscribe((event: KeyboardEvent) => this.handleKeyboardEvent(event));
            }
        });
    }

    private async handleKeyboardEvent(event: KeyboardEvent) {
        if (this.globalOptions && !this.keyboardShortcutsService?.shouldIgnoreShortcut(event)) {
            for (const option of this.globalOptions) {
                if (
                    option.keyboardShortcut &&
                    matchesShortcutCondition(event, option.keyboardShortcut)
                ) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (await option.enabledCallback()) {
                        this.ngZone.run(() => option.callback(null));
                    } else {
                        this.toast.error(null, 'TOAST.OPTION_DISABLED_OR_UNAVAILABLE');
                    }
                    break;
                }
            }
        }
    }
    filterOptions(options: OptionItem[], target: Target, objects: Node[] | any = null) {
        return (
            this.optionsHelperService?.filterOptions(options, target, this.getData(), objects) ||
            options
        );
    }
    /**
     * shortcut to simply disable all options on the given compoennts
     * @param actionbar
     * @param list
     */
    clearComponents(actionbar: ActionbarComponent, list: ListEventInterface<Node> = null) {
        if (list) {
            list.setOptions(null);
        }
        if (actionbar) {
            actionbar.options = [];
        }
    }

    pasteNode(nodes: Node[] = []) {
        this.optionsHelperService.pasteNode(this.components, this.getData(), true, nodes);
    }
}
