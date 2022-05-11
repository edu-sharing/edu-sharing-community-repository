import {Type} from '@angular/core';
import {BulkBehavior} from '../mds/mds.component';
import {MdsEditorWidgetBase} from '../mds-editor/widgets/mds-editor-widget-base';
import {
    MdsEditorWidgetPreviewComponent
} from './widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import {
    MdsEditorWidgetAuthorComponent
} from './widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import {
    MdsEditorWidgetVersionComponent
} from './widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import {
    MdsEditorWidgetChildobjectsComponent
} from './widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import {
    MdsEditorWidgetLinkComponent
} from './widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import {
    MdsEditorWidgetLicenseComponent
} from './widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import {
    MdsEditorWidgetFileUploadComponent
} from './widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import {
    MdsEditorWidgetTextComponent
} from './widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import {
    MdsEditorWidgetTinyMCE
} from './widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import {
    MdsEditorWidgetVCardComponent
} from './widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import {
    MdsEditorWidgetCheckboxComponent
} from './widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import {
    MdsEditorWidgetRadioButtonComponent
} from './widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import {
    MdsEditorWidgetCheckboxesComponent
} from './widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import {
    MdsEditorWidgetChipsComponent
} from './widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import {
    MdsEditorWidgetAuthorityComponent
} from './widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import {
    MdsEditorWidgetSelectComponent
} from './widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {
    MdsEditorWidgetSliderComponent
} from './widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import {
    MdsEditorWidgetDurationComponent
} from './widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import {
    MdsEditorWidgetTreeComponent
} from './widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import {
    MdsEditorWidgetFacetListComponent
} from './widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import {NativeWidgetClass} from './mds-editor-view/mds-editor-view.component';

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

export type InputStatus = 'VALID' | 'INVALID' | 'DISABLED' | 'PENDING';
export const NativeWidgets: {
    [widgetType in NativeWidgetType]: NativeWidgetClass;
} = {
    preview: MdsEditorWidgetPreviewComponent,
    author: MdsEditorWidgetAuthorComponent,
    version: MdsEditorWidgetVersionComponent,
    childobjects: MdsEditorWidgetChildobjectsComponent,
    maptemplate: MdsEditorWidgetLinkComponent,
    contributor: MdsEditorWidgetLinkComponent,
    license: MdsEditorWidgetLicenseComponent,
    fileupload: MdsEditorWidgetFileUploadComponent,
    workflow: null as null,
};

export const WidgetComponents: {
    [type in MdsWidgetType]: MdsEditorWidgetComponent;
} = {
    [MdsWidgetType.Text]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Number]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Email]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Date]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Month]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Color]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Textarea]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.TinyMCE]: MdsEditorWidgetTinyMCE,
    [MdsWidgetType.VCard]: MdsEditorWidgetVCardComponent,
    [MdsWidgetType.Checkbox]: MdsEditorWidgetCheckboxComponent,
    [MdsWidgetType.RadioHorizontal]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.RadioVertical]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.CheckboxHorizontal]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.CheckboxVertical]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueAuthorityBadges]: MdsEditorWidgetAuthorityComponent,
    [MdsWidgetType.Singleoption]: MdsEditorWidgetSelectComponent,
    [MdsWidgetType.Slider]: MdsEditorWidgetSliderComponent,
    [MdsWidgetType.Range]: MdsEditorWidgetSliderComponent,
    [MdsWidgetType.Duration]: MdsEditorWidgetDurationComponent,
    [MdsWidgetType.SingleValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.MultiValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.DefaultValue]: null,
    [MdsWidgetType.FacetList]: MdsEditorWidgetFacetListComponent,
};

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
