import { TranslateService } from '@ngx-translate/core';
import {
    AuthenticationService,
    MdsDefinition,
    MdsSort,
    MdsWidget,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { Injectable } from '@angular/core';
import { ListItem, ListItemType } from '../types/list-item';

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
     * There are always Columns in the result['Default'] but there can also be further types
     * @param translate
     * @param mdsSet
     * @param name
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
                        (columns as any)[column[0]] = column[1].map((c) => {
                            if (c.id.includes('.')) {
                                const split = c.id.split('.');
                                type = split[0] as ListItemType;
                                c.id = split[1];
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
                    }
                    break;
                }
            }
        }
        if (!columns['Default']?.length) {
            const defaultColumns = [];
            if (mdsSet !== null) {
                console.warn(
                    'mds does not define columns for ' + name + ', invalid configuration!',
                );
            }
            if (name === 'search' || name === 'collectionReferences') {
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
            }
            columns['Default'] = defaultColumns;
        }
        console.log(columns);
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
                if (template === undefined || w.template === template) {
                    return w;
                }
            }
        }
        return null;
    }

    constructor(
        private authentication: AuthenticationService,
        private translate: TranslateService,
    ) {}

    /**
     * Same as getWidget, but will also check the widget conditions
     * @param connector
     * @param properties
     * @param id
     * @param template
     * @param widgets
     */
    async getWidgetWithCondition(
        properties: any,
        id: string,
        template: string = null,
        widgets: any,
    ) {
        for (let w of widgets) {
            if (w.id == id) {
                if (
                    (template == null || w.template == template) &&
                    (await this.isWidgetConditionTrue(w, properties))
                ) {
                    return w;
                }
            }
        }
        return null;
    }

    async isWidgetConditionTrue(widget: any, properties: any) {
        if (!widget.condition) return true;
        let condition = widget.condition;
        if (condition.type == 'PROPERTY' && properties) {
            if (
                (!properties[condition.value] && !condition.negate) ||
                (properties[condition.value] && condition.negate)
            ) {
                return false;
            }
        }
        if (condition.type == 'TOOLPERMISSION') {
            let tp = await this.authentication.hasToolpermission(condition.value);
            if (tp == condition.negate) {
                return false;
            }
        }
        return true;
    }
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
     * Returns all widgets used by the given template
     */
    static getUsedWidgets(mds: MdsDefinition, template: string = null): any[] {
        const used: any = [];
        const templateData = MdsHelperService.findTemplate(mds, template);
        for (const w of mds.widgets) {
            if (
                templateData.html.indexOf('<' + w.id) !== -1 &&
                !used.find((w2: any) => w2.id === w.id)
            ) {
                used.push(w);
            }
        }
        return used;
    }
}

type ArrayElement<ArrayType extends readonly unknown[]> =
    ArrayType extends readonly (infer ElementType)[] ? ElementType : never;
