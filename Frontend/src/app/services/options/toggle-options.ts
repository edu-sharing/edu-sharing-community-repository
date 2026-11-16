import {
    DefaultGroups,
    ListEventInterface,
    NodeEntriesDisplayType,
    OptionItem,
    OptionItemToggle,
    Scope,
} from 'ngx-edu-sharing-ui';
import { SelectionModel } from '@angular/cdk/collections';
import { map } from 'rxjs/operators';
import { OptionsContext } from './options-context';

/**
 * The "select all" toggle that is currently offered for each list. The options are rebuilt on
 * every refresh, so the list is subscribed to only once and its events always update the toggle
 * of the latest build.
 */
const selectionToggles = new WeakMap<ListEventInterface<any>, { toggle: OptionItemToggle }>();

function registerSelectionToggle(list: ListEventInterface<any>, toggle: OptionItemToggle) {
    if (!list) {
        return;
    }
    let entry = selectionToggles.get(list);
    const isNew = !entry;
    if (isNew) {
        entry = { toggle };
        selectionToggles.set(list, entry);
    }
    entry.toggle = toggle;
    const updateVisibility = () => {
        // the table selects with its own header checkbox, and without checkboxes there is
        // nothing to select
        entry.toggle.isToggleVisible =
            list.getDisplayType() !== NodeEntriesDisplayType.Table &&
            list.hasCheckboxes?.() !== false;
    };
    const updateSelectionState = (selection: SelectionModel<any>) => {
        entry.toggle.toggleState = !selection?.isEmpty();
    };
    if (isNew) {
        list.getSelection()
            ?.changed.pipe(map((s) => s.source))
            .subscribe(updateSelectionState);
        list.onDisplayTypeChange().subscribe(updateVisibility);
        list.onCheckboxChange?.().subscribe(updateVisibility);
    }
    updateSelectionState(list.getSelection());
    updateVisibility();
}

export function createToggleOptions({
    service,
    management,
    components,
    data,
}: OptionsContext): OptionItem[] {
    const setDisplayType = (viewType: number, emit = true) => {
        switch (viewType) {
            case NodeEntriesDisplayType.Table:
                components.list.setDisplayType(NodeEntriesDisplayType.Table);
                toggleViewType.name = 'OPTIONS.SWITCH_TO_CARDS_VIEW';
                toggleViewType.icon = 'view_module';
                break;
            case NodeEntriesDisplayType.Grid:
                components.list.setDisplayType(NodeEntriesDisplayType.Grid);
                toggleViewType.name = 'OPTIONS.SWITCH_TO_LIST_VIEW';
                toggleViewType.icon = 'list';
                break;
        }
        if (emit) {
            service.displayTypeChanged.emit(components.list.getDisplayType());
        }
    };
    // enabled = table
    // disabled = grid
    const toggleViewType = new OptionItemToggle(
        {
            enabled: 'OPTIONS.SWITCH_TO_CARDS_VIEW',
            disabled: 'OPTIONS.SWITCH_TO_LIST_VIEW',
        },
        {
            enabled: 'view_module',
            disabled: 'list',
        },
        components?.list?.getDisplayType() === NodeEntriesDisplayType.Table,
        () => {
            switch (components.list.getDisplayType()) {
                case NodeEntriesDisplayType.Table:
                    setDisplayType(NodeEntriesDisplayType.Grid);
                    break;
                case NodeEntriesDisplayType.Grid:
                    setDisplayType(NodeEntriesDisplayType.Table);
                    break;
            }
        },
    );
    toggleViewType.scopes = [
        Scope.WorkspaceList,
        Scope.Search,
        // node browser
        Scope.Admin,
    ];
    toggleViewType.constrains = [];
    toggleViewType.group = DefaultGroups.Toggles;
    toggleViewType.elementType = [];
    toggleViewType.priority = 15;
    toggleViewType.togglePosition = 'before';

    const toggleSelection = new OptionItemToggle(
        {
            enabled: 'OPTIONS.DESELECT',
            disabled: 'OPTIONS.SELECT_ALL',
        },
        {
            enabled: 'deselect',
            disabled: 'select_all',
        },
        !components?.list?.getSelection()?.isEmpty(),
        () => {
            if (components.list?.getSelection()?.isEmpty()) {
                components.list?.selectAll();
            } else {
                components.list?.getSelection().clear();
            }
        },
    );
    registerSelectionToggle(components?.list, toggleSelection);
    toggleSelection.scopes = [Scope.WorkspaceList, Scope.Search, Scope.CollectionsReferences];
    toggleSelection.group = DefaultGroups.Toggles;
    toggleSelection.customShowCallback = async () => data?.allObjects?.length > 0;
    toggleSelection.elementType = [];
    toggleSelection.priority = 10;
    toggleSelection.togglePosition = 'before';

    return [toggleViewType, toggleSelection];
}
