import { ConfigWorkspaceColumns, WorkspaceColumnConfigEntry } from 'ngx-edu-sharing-api';
import { ListItem, NodeRoot } from 'ngx-edu-sharing-ui';
import { RestConnectorService, RestConstants } from '../../core-module/core.module';

/**
 * Base areas that may define their own column set via `<workspaceColumns><area>`.
 * `ALL_FILES` (the state while browsing/searching inside "my files") shares the `MY_FILES` set.
 */
export type WorkspaceColumnsArea = 'MY_FILES' | 'SHARED_FILES' | 'WORKFLOW_RECEIVE';

const DEFAULT_COLUMNS: WorkspaceColumnConfigEntry[] = [
    { id: RestConstants.CM_NAME, defaultVisibility: 'visible' },
    { id: RestConstants.CM_CREATOR, defaultVisibility: 'visible' },
    { id: RestConstants.CM_MODIFIED_DATE, defaultVisibility: 'visible' },
    { id: RestConstants.LOM_PROP_TITLE, defaultVisibility: 'hidden' },
    { id: RestConstants.LOM_PROP_SIZE, defaultVisibility: 'hidden' },
    { id: RestConstants.CM_PROP_C_CREATED, defaultVisibility: 'hidden' },
    { id: RestConstants.MEDIATYPE, defaultVisibility: 'hidden' },
    { id: RestConstants.LOM_PROP_GENERAL_KEYWORD, defaultVisibility: 'hidden' },
    { id: RestConstants.DIMENSIONS, defaultVisibility: 'hidden' },
    { id: RestConstants.LOM_PROP_LIFECYCLE_VERSION, defaultVisibility: 'hidden' },
    { id: RestConstants.VIRTUAL_PROP_USAGECOUNT, defaultVisibility: 'hidden' },
    { id: RestConstants.CCM_PROP_LICENSE, defaultVisibility: 'hidden' },
    { id: RestConstants.CCM_PROP_WF_STATUS, defaultVisibility: 'hidden' },
];

/**
 * Moves `id` to `position` and makes it visible - used to derive an area default from
 * {@link DEFAULT_COLUMNS} without repeating the whole list.
 */
function promote(id: string, position: number): WorkspaceColumnConfigEntry[] {
    const columns = DEFAULT_COLUMNS.filter((column) => column.id !== id);
    columns.splice(position, 0, { id, defaultVisibility: 'visible' });
    return columns;
}

/**
 * The column sets used when the client config does not provide `workspaceColumns`.
 * This is the only place workspace column defaults are defined - there is deliberately no
 * default in `client.config.xml`.
 */
export const WORKSPACE_COLUMNS_DEFAULT: ConfigWorkspaceColumns = {
    columns: DEFAULT_COLUMNS,
    areas: [
        {
            // "i am responsible": the workflow status is the key information of this area
            root: 'WORKFLOW_RECEIVE',
            columns: promote(RestConstants.CCM_PROP_WF_STATUS, 1),
        },
    ],
};

/** columns only offered to global admins, added unless the config already lists them */
const ADMIN_COLUMNS: WorkspaceColumnConfigEntry[] = [
    { id: RestConstants.NODE_ID, defaultVisibility: 'visible' },
    { id: RestConstants.CCM_PROP_REPLICATIONSOURCEID, defaultVisibility: 'hidden' },
];

export function mapWorkspaceColumnsArea(root: NodeRoot): WorkspaceColumnsArea {
    return root === 'SHARED_FILES' || root === 'WORKFLOW_RECEIVE' ? root : 'MY_FILES';
}

/**
 * Builds the workspace columns for a given base area.
 *
 * The config list defines which columns are *available* (i.e. offered in the column chooser) and
 * in which order; `defaultVisibility` decides whether a column starts out visible. Any node
 * property may be used - its label is resolved via the i18n key `NODE.<id>` by `esListItemLabel`.
 *
 * A previously stored user layout (`customColumns`) overrides visibility and order, but can never
 * re-introduce a column the config no longer offers.
 */
export function buildWorkspaceColumns({
    connector,
    config,
    root,
    customColumns,
}: {
    connector: RestConnectorService;
    config: ConfigWorkspaceColumns | null | undefined;
    root: NodeRoot;
    customColumns?: ListItem[] | null;
}): ListItem[] {
    const columns = resolveEntries(config, mapWorkspaceColumnsArea(root))
        .filter((entry) => !!entry.id)
        .map(toListItem);
    if (connector.getCurrentLogin()?.isAdmin) {
        // placed behind the last visible column so they do not push configured columns out of view
        let insertAt = columns.length;
        while (insertAt > 0 && !columns[insertAt - 1].visible) {
            insertAt--;
        }
        for (const entry of ADMIN_COLUMNS) {
            if (!columns.some((column) => column.name === entry.id)) {
                columns.splice(insertAt++, 0, toListItem(entry));
            }
        }
    }
    return applyUserLayout(columns, customColumns);
}

function toListItem(entry: WorkspaceColumnConfigEntry): ListItem {
    const column = new ListItem('NODE', entry.id);
    column.visible = entry.defaultVisibility !== 'hidden';
    return column;
}

/**
 * A configured column list always wins as a whole: an area override replaces the global list, and
 * a global list replaces the frontend defaults. Only if the config says nothing at all about an
 * area do the frontend defaults apply.
 */
function resolveEntries(
    config: ConfigWorkspaceColumns | null | undefined,
    area: WorkspaceColumnsArea,
): WorkspaceColumnConfigEntry[] {
    return (
        areaColumns(config, area) ??
        (config?.columns?.length ? config.columns : null) ??
        areaColumns(WORKSPACE_COLUMNS_DEFAULT, area) ??
        WORKSPACE_COLUMNS_DEFAULT.columns
    );
}

function areaColumns(
    config: ConfigWorkspaceColumns | null | undefined,
    area: WorkspaceColumnsArea,
): WorkspaceColumnConfigEntry[] | null {
    const columns = config?.areas?.find((entry) => entry.root === area)?.columns;
    return columns?.length ? columns : null;
}

/**
 * Applies the stored user layout: known columns take over the user's visibility and position,
 * columns the user never saw keep their config default and stay at their config position.
 * Entries of the stored layout that are no longer available are dropped.
 */
function applyUserLayout(columns: ListItem[], customColumns?: ListItem[] | null): ListItem[] {
    if (!Array.isArray(customColumns) || !customColumns.length) {
        return columns;
    }
    const order = customColumns.map((column) => column.name);
    for (const column of columns) {
        const stored = customColumns.find((c) => c.name === column.name);
        if (stored) {
            column.visible = stored.visible;
        }
    }
    // columns unknown to the stored layout keep their relative order at the end
    const position = (column: ListItem) => {
        const pos = order.indexOf(column.name);
        return pos === -1 ? order.length : pos;
    };
    return [...columns].sort((a, b) => position(a) - position(b));
}
