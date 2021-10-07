import { Type } from '@angular/core';
import { Sort } from '../../../core-module/core.module';
import { BulkBehavior } from '../mds/mds.component';
import { MdsEditorWidgetBase } from './widgets/mds-editor-widget-base';

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

/**
 * - `nodes`:
 *   - Supports bulk.
 *   - Returns only changed values.
 * - `search`:
 *   - No bulk.
 *   - All values returned.
 *   - Trees sub-children are auto-selected if root is selected.
 *   - Required errors and -warnings are disabled.
 * - `form`:
 *   - No bulk.
 *   - All values returned.
 */
export type EditorMode = 'nodes' | 'search' | 'form';

export type ViewRelation = 'suggestions';

export interface Constraints {
    requiresNode?: boolean;
    supportsBulk?: boolean;
}

export type Values = { [property: string]: string[] };

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
}

export type MdsEditorWidgetComponent = Type<MdsEditorWidgetBase>;

export interface MdsDefinition {
    create: null;
    name: string;
    lists: MdsList[];
    groups: MdsGroup[];
    views: MdsView[];
    widgets: MdsWidget[];
    sorts: Sort[];
}

export interface MdsView {
    id: string;
    caption: string;
    isExtended: boolean;
    html: string;
    icon: string;
    rel: ViewRelation;
    hideIfEmpty: boolean;
}

export type EditorType = 'angular' | 'legacy';

export interface MdsGroup {
    id: string;
    views: string[];
    rendering: EditorType;
}

export interface MdsList {
    columns: MdsListColumn[];
    id: string;
}

export interface MdsListColumn {
    format: null;
    id: string;
    showDefault: boolean;
}

// Incomplete, fill in as needed.
export interface MdsWidget {
    link: null;
    subwidgets: null;
    condition: MdsWidgetCondition;
    maxlength: number;
    id: string;
    caption: string;
    bottomCaption: string;
    icon: null;
    type: MdsWidgetType;
    template: null;
    hasValues: boolean;
    values: MdsWidgetValue[];
    placeholder: null;
    unit: string;
    min: number;
    max: number;
    defaultMin: number;
    defaultMax: number;
    step: number;
    allowempty: boolean;
    defaultvalue: string;
    isRequired: RequiredMode;
    isSearchable: boolean;
    isExtended: boolean|string;
    hideIfEmpty: boolean;
    interactionType: 'Input' | 'None';
}

// Incomplete, fill in as needed.
export interface MdsWidgetValue {
    id: string;
    caption: string;
    description?: string;
    parent?: string;
}
export type MdsWidgetFacetValue = MdsWidgetValue & { count: number };

export interface MdsWidgetCondition {
    dynamic: boolean;
    type: 'PROPERTY' | 'TOOLPERMISSION';
    value: string;
    negate: boolean;
    pattern?: string;
}

export type FacetValues = { [property: string]: MdsWidgetFacetValue[] };

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
