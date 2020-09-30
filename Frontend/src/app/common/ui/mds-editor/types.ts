import { Type } from '@angular/core';
import { View, Sort } from '../../../core-module/core.module';
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

export interface Constraints {
    requiresNode?: boolean;
    supportsBulk?: boolean;
}

export type Values = { [property: string]: string[] };

export type BulkMode = 'no-change' | 'replace';

export type InputStatus = 'VALID' | 'INVALID' | 'DISABLED' | 'PENDING';

export enum MdsType {
    Io = 'io',
    IoBulk = 'io_bulk',
    Map = 'map',
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
    Singleoption = 'singleoption',
    Slider = 'slider',
    Range = 'range',
    Duration = 'duration',
    SingleValueTree = 'singlevalueTree',
    MultiValueTree = 'multivalueTree',
    DefaultValue = 'defaultvalue',
}

export enum NativeWidgetType {
    Preview = 'preview',
    Version = 'version',
    ChildObjects = 'childobjects',
    Template = 'template',
    License = 'license',
    Workflow = 'workflow',
    Author = 'author',
}

export type MdsEditorWidgetComponent = Type<MdsEditorWidgetBase>;

export interface MdsDefinition {
    create: null;
    name: string;
    lists: MdsList[];
    groups: MdsGroup[];
    views: View[];
    widgets: MdsWidget[];
    sorts: Sort[];
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
    isExtended: boolean;
}

// Incomplete, fill in as needed.
export interface MdsWidgetValue {
    id: string;
    caption: string;
    description?: string;
    parent?: string;
}

export interface MdsWidgetCondition {
    type: 'PROPERTY' | 'TOOLPERMISSION';
    value: string;
    negate: boolean;
}

export enum RequiredMode {
    Mandatory = 'mandatory',
    MandatoryForPublish = 'mandatoryForPublish',
    Optional = 'optional',
}

export function assertUnreachable(x: never): never {
    throw new Error('Did not expect to get here');
}
