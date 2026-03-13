import { TranslateService } from '@ngx-translate/core';
import {
    AuthenticationService,
    MdsDefinition,
    MdsIdentifier,
    MdsService,
    MdsSort,
    MdsWidget,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { Injectable } from '@angular/core';
import { ListItem, ListItemType } from '../types/list-item';
import { isArray } from 'lodash';
import { firstValueFrom } from 'rxjs';

type ColumnTypeInternal<T extends string> = { [k in T]?: ListItem[] };
export type ColumnType = ColumnTypeInternal<'Default' | 'Table'>;
@Injectable()
export class MdsHelperService {
    static getSortInfo(mdsSet: MdsDefinition, name: string): MdsSort {
        if (mdsSet) {
            if (mdsSet.sorts) {
                for (const list of mdsSet.sorts) {
                    if (list.id == name) {
                        return list;
                    }
                }
            }
            console.error('mds does not define sort info for ' + name + ', invalid configuration!');
        }
        return null;
    }

    /**
     *
     * get columns as an object structure
     * There are always Columns in the result.Default but there can also be further types
     * See @ColumnType
     */
    getColumns(mdsSet: MdsDefinition, name: string) {
        let columns: ColumnType = {};
        if (mdsSet) {
            for (const list of mdsSet.lists) {
                if (list.id === name) {
                    for (const column of Object.entries(list.columns)) {
                        let type: ListItemType = 'NODE';
                        if (name === 'mediacenterGroups') {
                            type = 'GROUP';
                        } else if (name === 'searchCollections') {
                            type = 'COLLECTION';
                        }
                        if (Array.isArray(column[1])) {
                            (columns as any)[column[0]] = column[1].map((c) => {
                                if (c.id.includes('.')) {
                                    const split = c.id.split('.');
                                    type = split[0] as ListItemType;
                                    c.id = split.slice(1).join('.');
                                }
                                const item = new ListItem(type, c.id);
                                item.format = c.format;
                                const key = item.type + '.' + item.name;
                                if (item.type === 'NODE' && this.translate.instant(key) === key) {
                                    item.label = mdsSet.widgets.filter(
                                        (w: any) => w.id === item.name,
                                    )?.[0]?.caption;
                                }
                                return item;
                            });
                        } else {
                            console.warn('Invalid column data from backend', column[0], column[1]);
                        }
                    }
                    break;
                }
            }
        }
        if (!columns?.Default?.length) {
            const defaultColumns = [];
            if (mdsSet !== null) {
                console.warn(
                    'mds does not define columns for ' + name + ', invalid configuration!',
                );
            }
            if (name === 'searchCollections' || name === 'swimlane_collections') {
                defaultColumns.push(new ListItem('COLLECTION', 'title'));
                defaultColumns.push(new ListItem('COLLECTION', 'info'));
                defaultColumns.push(new ListItem('COLLECTION', 'scope'));
            } else if (name === 'swimlane_assignments') {
                defaultColumns.push(new ListItem('ASSIGNMENT', null));
            } else if (
                ['search', 'collectionReferences', 'genericWidget', 'genericWidgetTable'].includes(
                    name,
                ) ||
                name.startsWith('swimlane_')
            ) {
                defaultColumns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
                defaultColumns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
                defaultColumns.push(new ListItem('NODE', RestConstants.CCM_PROP_LICENSE));
                defaultColumns.push(new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE));
            } else if (name === 'mediacenterManaged') {
                defaultColumns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
                defaultColumns.push(
                    new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCEID),
                );
                defaultColumns.push(new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE));
            } else if (name === 'mediacenterGroups') {
                defaultColumns.push(new ListItem('GROUP', RestConstants.AUTHORITY_DISPLAYNAME));
                defaultColumns.push(new ListItem('GROUP', RestConstants.AUTHORITY_GROUPTYPE));
            } else if (name === 'searchCollections') {
                defaultColumns.push(...ListItem.getCollectionDefaults());
            } else {
                defaultColumns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
            }
            columns['Default'] = defaultColumns;
        }
        return columns;
    }

    /**
     * Finds the appropriate widget with the id, but will not check any widget conditions
     * @param cid
     * @param template
     * @param widgets
     */
    static getWidget(cid: string, template: string | undefined | null, widgets: MdsWidget[]) {
        if (widgets == null) {
            console.warn('Could not iterate widget ' + cid + ': no widgets data provided');
            return null;
        }
        for (let w of widgets) {
            if (w.id == cid) {
                if (template === undefined || w.template?.includes(template)) {
                    return w;
                }
            }
        }
        return null;
    }

    constructor(
        private authentication: AuthenticationService,
        private mdsService: MdsService,
        private translate: TranslateService,
    ) {}

    /**
     * Find a template by id in the given mds
     */
    static findTemplate(mds: MdsDefinition, id: string) {
        return (
            mds.views as Array<
                ArrayElement<MdsDefinition['views']> | ArrayElement<MdsDefinition['views']>
            >
        ).find((v) => v.id === id);
    }

    /**
     * Same as getColumns, but you don't need to fetch the mds yourself
     */
    async getColumnsByMdsId(name: string, mds: Partial<MdsIdentifier>) {
        return this.getColumns(await firstValueFrom(this.mdsService.getMetadataSet(mds)), name);
    }
}

type ArrayElement<ArrayType extends readonly unknown[]> =
    ArrayType extends readonly (infer ElementType)[] ? ElementType : never;
