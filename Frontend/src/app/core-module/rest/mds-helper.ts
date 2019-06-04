import {ListItem} from "../core.module"
import {RestConstants} from "./rest-constants";

export class MdsHelper{
    static getSortInfo(mdsSet: any, name: string) {
        if(mdsSet) {
            if (mdsSet.sorts) {
                for (let list of mdsSet.sorts) {
                    if (list.id == name) {
                        return list;
                    }
                }
             }
            console.error('mds does not define sort info for ' + name + ', invalid configuration!');
        }
        return null;
    }
  static getColumns(mdsSet: any, name: string) {
    let columns:ListItem[]=[];
    if(mdsSet) {
      for (let list of mdsSet.lists) {
        if (list.id == name) {
          for (let column of list.columns) {
            let item = new ListItem("NODE", column.id)
            item.format = column.format;
            columns.push(item);
          }
          return columns;
        }
      }
      console.warn('mds does not define columns for ' + name + ', invalid configuration!');
    }
    if(name=='search' || name=='collectionReferences') {
      columns.push(new ListItem("NODE", RestConstants.CM_PROP_TITLE));
      columns.push(new ListItem("NODE", RestConstants.CM_MODIFIED_DATE));
      columns.push(new ListItem("NODE", RestConstants.CCM_PROP_LICENSE));
      columns.push(new ListItem("NODE", RestConstants.CCM_PROP_REPLICATIONSOURCE));
    }
    return columns;
  }
}
