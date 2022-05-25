import {Type} from '@angular/core';
import {MdsEditorWidgetBase} from '../mds-editor/widgets/mds-editor-widget-base';

export {
    MdsDefinition,
    MdsGroup,
    MdsValue as MdsWidgetValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
} from 'ngx-edu-sharing-api';

/** Error with a translatable message that is suitable to be shown to the user. */
export class UserPresentableError extends Error {
    constructor(message: string) {
        super(message);
        // Apparently, this is what it's gonna be for now...
        // https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
        Object.setPrototypeOf(this, UserPresentableError.prototype);
        this.name = 'UserPresentableError';
    }
}

export interface Constraints {
    supportsInlineEditing?: boolean;
    requiresNode?: boolean;
    supportsBulk?: boolean;
}

export type Values = { [property: string]: (string[] | null) };

/** User-selectable Bulk mode per field */
export type BulkMode = 'no-change' | 'replace';

/** Bulk mode and -behavior of the editor. */
export type EditorBulkMode =
    | {
          isBulk: false;
      }
    // The user toggles editing per-field.
    | {
          isBulk: true;
          bulkBehavior: BulkBehavior.Default;
      }
    // All fields are replaced.
    | {
          isBulk: true;
          bulkBehavior: BulkBehavior.Replace;
      };

export type InputStatus = 'VALID' | 'INVALID' | 'DISABLED' | 'PENDING';

export enum MdsType {
    Io = 'io',
    IoBulk = 'io_bulk',
    Map = 'map',
    MapRef = 'map_ref',
    IoChildObject = 'io_childobject',
    Collection = 'collection',
    ToolDefinition = 'tool_definition',
    ToolInstance = 'tool_instance',
    SavedSearch = 'saved_search',
}

export enum MdsWidgetType {
    Text = 'text',
    Number = 'number',
    Email = 'email',
    Date = 'date',
    Month = 'month',
    Color = 'color',
    Textarea = 'textarea',
    TinyMCE = 'tinyMCE',
    VCard = 'vcard',
    Checkbox = 'checkbox',
    RadioHorizontal = 'radioHorizontal',
    RadioVertical = 'radioVertical',
    CheckboxHorizontal = 'checkboxHorizontal',
    CheckboxVertical = 'checkboxVertical',
    MultiValueBadges = 'multivalueBadges',
    MultiValueFixedBadges = 'multivalueFixedBadges',
    MultiValueSuggestBadges = 'multivalueSuggestBadges',
    MultiValueAuthorityBadges = 'multivalueAuthorityBadges',
    Singleoption = 'singleoption',
    Slider = 'slider',
    Range = 'range',
    Duration = 'duration',
    SingleValueTree = 'singlevalueTree',
    MultiValueTree = 'multivalueTree',
    DefaultValue = 'defaultvalue',
    FacetList = 'facetList',
}

// Entries must be lowercase only.
export enum NativeWidgetType {
    Preview = 'preview',
    Version = 'version',
    ChildObjects = 'childobjects',
    Maptemplate = 'maptemplate',
    License = 'license',
    FileUpload = 'fileupload',
    Workflow = 'workflow',
    Author = 'author',
    Contributor = 'contributor',
}

export type MdsEditorWidgetComponent = Type<MdsEditorWidgetBase>;

export type EditorType = 'angular' | 'legacy';

export interface MdsList {
    columns: MdsListColumn[];
    id: string;
}

export interface MdsListColumn {
    format: null;
    id: string;
    showDefault: boolean;
}

export enum RequiredMode {
    Mandatory = 'mandatory',
    MandatoryForPublish = 'mandatoryForPublish',
    Recommended = 'recommended',
    Optional = 'optional',
    Ignore = 'ignore',
}

export function assertUnreachable(x: never): never {
    throw new Error('Did not expect to get here');
}

export enum BulkBehavior {
    Default, // default equals no replace on choose, but show options
    Replace, // Don't display settings, simply replace for all (usefull after uploads)
}
