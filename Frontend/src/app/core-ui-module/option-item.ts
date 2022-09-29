/**
 * A option from the actionbar+dropdown menu
 * Example:
 this.options.push(new OptionItem("RESTORE_SINGLE","undo", (node) => this.restoreSingle(node)));
 this.options.push(new OptionItem("DELETE_SINGLE","archiveDelete", (node) => this.deleteSingle(node)));

 */
import {NodesRightMode} from '../core-module/rest/data-object';

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
   * If true, shows a line at the top.
   *
   * This feature is usually handled now by associating an entry to a `group`
   * now. However `isSeparate` is kept as fallback for now.
   */
  public isSeparate = false;
  /**
   * The item will be indicated with an accent color in the menu.
   *
   * Used by the dropdown variant of the main menu to indicate the currently
   * active scope.
   */
  public isSelected = false;
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
   * A function called with the node as param which should return true or false if the option should be shown for this node
   * Is handled by optionsHelper and may not be used otherwise
   * Please use @customShowCallback instead
   */
  public showCallback : (node?: Node|any) => boolean;
  /**
   * A function called with the node as parm which should return true or false if the option should be shown for this node
   * Will be called by the optionsHelper
   */
  public customShowCallback : (nodes?: Node[]|any[]) => boolean;
  /**
   * A function called with the node as param which should return true or false if the option should be enabled or not
   * Is handled by optionsHelper and may not be used otherwise
   * Please use @customEnabledCallback instead
   */
  public enabledCallback : (node?: Node|any) => boolean;
  /**
   * A function called with the node as param which should return true or false if the option should be enabled or not
   * Will be called by the optionsHelper
   */
  public customEnabledCallback : (nodes?: Node[]|any[]) => boolean;

  /**
   *   Optional: A callback that is called when the user clicks on the option when it's currently disabled (greyed out)
   */
  public disabledCallback : (node?: Node|any) => void;
  /**
   *   Show the given name (if false, only icon will show)
   */
  public showName = true;

  /**
   * element with 0 has highest priority in it's group, higher values mean lower priority
   */
  public priority: number;
  /**
   * the group this option belongs to
   */
  public group: OptionGroup;

  public key: string;
  public keyCombination: KeyCombination[];
  /**
   * Or concat of supported element types for the action
   */
  public elementType = [ElementType.Node];
  public permissions: string[];
  public constrains: Constrain[];
  public permissionsMode = HideMode.Disable;
  public permissionsRightMode = NodesRightMode.Local;
  public toolpermissions: string[];
  public toolpermissionsMode = HideMode.Disable;
  public scopes: Scope[];

  /**
   *
   * @param name the option name, which is used for the translation
   * @param icon A material icon name
   * @param callback A function callback when this option is choosen. Will get the current node passed as an argument
   */
  constructor(public name: string, public icon: string, public callback: (object?: Node|any) => void) {
  }
}
export class CustomOptions {
    /**
     * If true, all existing or available options for the object stay
     * If false, no options will be set, only the options in "addOptions" are used
     */
    public useDefaultOptions? = true;
    /**
     * List of ids of options to explicitly support
     */
    public supportedOptions?: string[] = [];
    /**
     * List of ids of options to explicitly remove
     * Only supported if supportedOptions is empty
     */
    public removeOptions?: string[] = [];
    /**
     * Options to add/insert into the menu
     */
    public addOptions?: OptionItem[] = [];
}
export enum HideMode {
  Disable,
  Hide
}
export enum Scope {
  Render = 'Render',
  Search = 'Search',
  CollectionsReferences = 'CollectionsReferences',
  CollectionsCollection = 'CollectionsCollection',
  WorkspaceList = 'WorkspaceList',
  WorkspaceTree = 'WorkspaceTree',
  Oer = 'Oer',
  CreateMenu = 'CreateMenu',
  Admin = 'Admin', // Admin Tools / Debugging
}
export enum ElementType {
  Node,
  NodeChild, // Child object
  MapRef, // Map ref (link to another map)
  NodePublishedCopy,
  NodeBlockedImport, // node with property ccm:importblocked == true
  Person,
  Group,
  SavedSearch,
  Unknown
}
export class OptionGroup {
  /**
   * @param id The group identifier. Used to seperate elements based on the group
   * @param priority Group with 0 has highest priority, higher values mean lower priority
   */
  constructor(public id: string, public priority: number) {
  }
}
export class DefaultGroups {
  static Primary = new OptionGroup('Primary', 10);
  static Create = new OptionGroup('Create', 15);
  static View = new OptionGroup('View', 20);
  static CreateConnector = new OptionGroup('CreateConnector', 25);
  static Reuse = new OptionGroup('Reuse', 30);
  static Edit = new OptionGroup('Edit', 40);
  static FileOperations = new OptionGroup('FileOperations', 50);
  static Delete = new OptionGroup('Delete', 60);
  static Toggles = new OptionGroup('Toggles', 70);
}
export enum Constrain {
  CollectionReference, // option is only visible for collection references
  NoCollectionReference, // option is only visible for non-collection references
  Directory, // only visible for directories (ccm:map)
  Collections, // only visible for collections
  Files, // only visible for files (ccm:io)
  FilesAndDirectories, // only visible for files and directories (ccm:io / ccm:map) - no collections!
  Admin, // only visible if user is admin or esDebug is enabled on window component
  AdminOrDebug, // only visible if user is admin or esDebug is enabled on window component
  NoBulk, // No support for bulk (multiple objects)
  NoSelection, // Only visible when currently no element is selected
  ClipboardContent, // Only visible when the clipboard has content
  AddObjects, // Only visible when it is possible to add objects into the current list
  HomeRepository, // Only visible when the nodes are from the local (home) repository
  User, // Only visible when a user is present and logged in
  NoScope, // Only visible when no current scope (i.e "safe" scope) is set
  ReurlMode, // Only visible when a reurl is present (called to pick object from lms)
}
export enum KeyCombination {
  CtrlOrAppleCmd
}
export enum Target {
  List, // Target is the ListTableComponent
  ListDropdown,
  Actionbar,
  CreateMenu
}
