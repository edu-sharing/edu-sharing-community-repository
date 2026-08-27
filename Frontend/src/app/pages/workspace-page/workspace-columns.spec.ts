import { ListItem } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../core-module/core.module';
import { buildWorkspaceColumns, mapWorkspaceColumnsArea } from './workspace-columns';

/** minimal stand-in for the parts of RestConnectorService the builder uses */
function connectorMock(isAdmin = false) {
    return { getCurrentLogin: () => ({ isAdmin }) } as any;
}

function names(columns: ListItem[]) {
    return columns.map((column) => column.name);
}

function visibleNames(columns: ListItem[]) {
    return columns.filter((column) => column.visible).map((column) => column.name);
}

describe('mapWorkspaceColumnsArea', () => {
    it('maps browsing/searching inside my files to MY_FILES', () => {
        void expect(mapWorkspaceColumnsArea('MY_FILES')).toBe('MY_FILES');
        void expect(mapWorkspaceColumnsArea('ALL_FILES')).toBe('MY_FILES');
        void expect(mapWorkspaceColumnsArea('RECYCLE')).toBe('MY_FILES');
    });

    it('keeps the areas that may be configured on their own', () => {
        void expect(mapWorkspaceColumnsArea('SHARED_FILES')).toBe('SHARED_FILES');
        void expect(mapWorkspaceColumnsArea('WORKFLOW_RECEIVE')).toBe('WORKFLOW_RECEIVE');
    });
});

describe('buildWorkspaceColumns', () => {
    it('uses the frontend defaults when the config says nothing', () => {
        const columns = buildWorkspaceColumns({
            connector: connectorMock(),
            config: null,
            root: 'MY_FILES',
        });
        void expect(visibleNames(columns)).toEqual([
            RestConstants.CM_NAME,
            RestConstants.CM_CREATOR,
            RestConstants.CM_MODIFIED_DATE,
        ]);
        // hidden columns are still available in the column chooser
        void expect(names(columns)).toContain(RestConstants.CCM_PROP_WF_STATUS);
    });

    it('shows the workflow status by default in the WORKFLOW_RECEIVE area', () => {
        const columns = buildWorkspaceColumns({
            connector: connectorMock(),
            config: null,
            root: 'WORKFLOW_RECEIVE',
        });
        void expect(visibleNames(columns)).toEqual([
            RestConstants.CM_NAME,
            RestConstants.CCM_PROP_WF_STATUS,
            RestConstants.CM_CREATOR,
            RestConstants.CM_MODIFIED_DATE,
        ]);
    });

    it('allows arbitrary properties and honours the configured order and visibility', () => {
        const columns = buildWorkspaceColumns({
            connector: connectorMock(),
            config: {
                columns: [
                    { id: RestConstants.CM_NAME, defaultVisibility: 'visible' },
                    { id: 'ccm:university', defaultVisibility: 'visible' },
                    { id: RestConstants.CM_CREATOR, defaultVisibility: 'hidden' },
                ],
            },
            root: 'MY_FILES',
        });
        void expect(names(columns)).toEqual([
            RestConstants.CM_NAME,
            'ccm:university',
            RestConstants.CM_CREATOR,
        ]);
        void expect(visibleNames(columns)).toEqual([RestConstants.CM_NAME, 'ccm:university']);
    });

    it('replaces the global list with the matching area override', () => {
        const config = {
            columns: [{ id: RestConstants.CM_NAME, defaultVisibility: 'visible' as const }],
            areas: [
                {
                    root: 'WORKFLOW_RECEIVE',
                    columns: [
                        { id: RestConstants.CM_NAME, defaultVisibility: 'visible' as const },
                        {
                            id: RestConstants.CCM_PROP_WF_STATUS,
                            defaultVisibility: 'visible' as const,
                        },
                    ],
                },
            ],
        };
        void expect(
            names(
                buildWorkspaceColumns({
                    connector: connectorMock(),
                    config,
                    root: 'WORKFLOW_RECEIVE',
                }),
            ),
        ).toEqual([RestConstants.CM_NAME, RestConstants.CCM_PROP_WF_STATUS]);
        // an area without an override falls back to the global list
        void expect(
            names(
                buildWorkspaceColumns({ connector: connectorMock(), config, root: 'SHARED_FILES' }),
            ),
        ).toEqual([RestConstants.CM_NAME]);
    });

    it('adds the admin columns behind the visible ones', () => {
        const columns = buildWorkspaceColumns({
            connector: connectorMock(true),
            config: null,
            root: 'MY_FILES',
        });
        void expect(names(columns).slice(0, 5)).toEqual([
            RestConstants.CM_NAME,
            RestConstants.CM_CREATOR,
            RestConstants.CM_MODIFIED_DATE,
            RestConstants.NODE_ID,
            RestConstants.CCM_PROP_REPLICATIONSOURCEID,
        ]);
        void expect(visibleNames(columns)).toContain(RestConstants.NODE_ID);
        void expect(visibleNames(columns)).not.toContain(
            RestConstants.CCM_PROP_REPLICATIONSOURCEID,
        );
    });

    it('applies the stored user layout for visibility and order', () => {
        const stored = [
            Object.assign(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE), {
                visible: false,
            }),
            Object.assign(new ListItem('NODE', RestConstants.CCM_PROP_LICENSE), { visible: true }),
        ];
        const columns = buildWorkspaceColumns({
            connector: connectorMock(),
            config: null,
            root: 'MY_FILES',
            customColumns: stored,
        });
        void expect(names(columns).slice(0, 2)).toEqual([
            RestConstants.CM_MODIFIED_DATE,
            RestConstants.CCM_PROP_LICENSE,
        ]);
        void expect(visibleNames(columns)).toEqual([
            RestConstants.CCM_PROP_LICENSE,
            RestConstants.CM_NAME,
            RestConstants.CM_CREATOR,
        ]);
    });

    it('drops stored columns that the config no longer offers', () => {
        const stored = [
            Object.assign(new ListItem('NODE', 'ccm:removed_column'), { visible: true }),
            Object.assign(new ListItem('NODE', RestConstants.CM_NAME), { visible: true }),
        ];
        const columns = buildWorkspaceColumns({
            connector: connectorMock(),
            config: { columns: [{ id: RestConstants.CM_NAME, defaultVisibility: 'visible' }] },
            root: 'MY_FILES',
            customColumns: stored,
        });
        void expect(names(columns)).toEqual([RestConstants.CM_NAME]);
    });
});
