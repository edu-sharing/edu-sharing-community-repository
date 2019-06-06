/**
 * A option from the actionbar+dropdown menu
 * Example:
 this.options.push(new OptionItem("RESTORE_SINGLE","undo", (node) => this.restoreSingle(node)));
 this.options.push(new OptionItem("DELETE_SINGLE","archiveDelete", (node) => this.deleteSingle(node)));

 */

export class OptionItem {
  /**
   * If true, this option will be shown all the time in the node table
   * @type {boolean}
   */
  public showAlways = false;
  /**
   * If true, this option will be shown as an action (if room). If none of the items has showAsAction set, the first items will always be shown as action
   * @type {boolean}
   */
  public showAsAction = false;
  /**
   * If true, this option will be shown as a toggle on the right side (provide iconToggle as a toggle icon)
   * @type {boolean}
   */
  public isToggle = false;
  /**
   * If true, shows a line at the top
   * @type {boolean}
   */
  public isSeperate = false;
  /**
   * Like @isSeperate, but shows a line at bottom
   * @type {boolean}
   */
  public isSeperateBottom = false;
  /**
   * If true, only displayed on a mobile device (based on the navigator agent)
   * @type {boolean}
   */
  public onlyMobile = false;
    /**
     * If true, only displayed on a desktop device (based on the navigator agent)
     * @type {boolean}
     */
    public onlyDesktop = false;
  /**
   * You can set a media-query similar to CSS, see the MEDIA_QUERY constants in ui-constants
   * Use in combination with mediaQueryValue
   * @type {boolean}
   */
  public mediaQueryType : string;
  /**
   * The value for the defined media query
   */
  public mediaQueryValue : number;

  /**
   * Set to false if the action should be shown, but should not be clickable
   * @type {boolean}
   */
  public isEnabled = true;

  /**
   * A function called with the node in content which should return true or false if the option should be shown for this node
   */
  public showCallback : Function;
  /**
   * A function called with the node in content which should return true or false if the option should be enabled or not
   */
  public enabledCallback : Function;

  /**
   *   Optional: A callback that is called when the user clicks on the option when it's currently disabled (greyed out)
   */
  public disabledCallback : Function;
  /**
   *   Show the given name (if false, only icon will show)
   */
  public showName = true;
  /**
   *
   * @param name the option name, which is used for the translation
   * @param icon A material icon name
   * @param callback A function callback when this option is choosen. Will get the current node passed as an argument
   */
  constructor(public name: string, public icon: string, public callback: Function) {
  }
}
