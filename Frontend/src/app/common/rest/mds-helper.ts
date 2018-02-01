import {ListItem} from "../ui/list-item";
import {RestConstants} from "./rest-constants";
import {Authority, LocalPermissions, Permission} from "./data-object";

export class MdsHelper{

  static getColumns(mdsSet: any, name: string) {
    let columns=[];
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
      console.info('mds does not define columns for ' + name + ', using defaults');
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
