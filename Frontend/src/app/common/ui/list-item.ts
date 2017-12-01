/**
 * A list item info, which is basically a column
 * Example:
 this.columns.push(new ListItem(RestConstants.CM_NAME));
 this.columns.push(new ListItem(RestConstants.CM_ARCHIVED_DATE));
 */
export class ListItem{
  /**
   * Should this item be shown by default
   * @type {boolean}
   */
  public visible=true;
  constructor(public type : string,public name : string) {
  }
}
