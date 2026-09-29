import { Component, Input, OnInit, Optional } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionItem, OptionsHelperDataService, Target } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { Constraints } from '../../../types/types';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { Attributes } from '../../util/parse-attributes';

/** the option to render, together with the node it was resolved for */
export interface ResolvedAction {
    option: OptionItem;
    node: Node;
}

/**
 * Renders an option of the options helper as a button inside an mds template.
 *
 * Generic replacement for the action widgets the backend `MetadataTemplateRenderer` renders
 * on its own (e.g. `material_feedback`). Instead of re-implementing the permission,
 * toolpermission and scope checks, the option is looked up in the list the options helper
 * already filtered for the current node — if it is not in there, nothing is rendered.
 *
 * Usage in an mds template, optionally overriding the option's label and icon:
 * ```
 * <action option="OPTIONS.MATERIAL_FEEDBACK">
 * <action option="OPTIONS.MATERIAL_FEEDBACK" caption="Rückmeldung geben" icon="feedback">
 * ```
 */
@Component({
    selector: 'es-mds-editor-widget-action',
    templateUrl: './mds-editor-widget-action.component.html',
    styleUrls: ['./mds-editor-widget-action.component.scss'],
    standalone: false,
})
export class MdsEditorWidgetActionComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints: Constraints = {
        requiresNode: true,
        supportsBulk: false,
        // an unavailable option is a permission result, not a misconfiguration
        onConstraintFailed: 'hide',
    };

    @Input() attributes: Attributes;
    /** the mds widget, if the tag has a definition in the mds */
    @Input() widget: Widget;

    hasChanges = new BehaviorSubject<boolean>(false);
    action$: Observable<ResolvedAction | null>;

    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        @Optional() private optionsHelper: OptionsHelperDataService,
    ) {}

    ngOnInit(): void {
        // switchMap drops the result of a lookup that a newer node already made obsolete
        this.action$ = this.mdsEditorValues.nodes$.pipe(
            switchMap((nodes) => this.resolveAction(nodes?.[0])),
        );
    }

    /**
     * Caption override from the mds, falling back to the option's name in the template.
     *
     * Captions of the mds are already translated when the mds is read, so unlike
     * `option.name` this must not be passed through the translate pipe.
     */
    get caption(): string | null {
        return this.attributes?.['caption'] ?? this.widget?.definition?.caption ?? null;
    }

    /** icon override from the mds, falling back to the option's icon */
    icon(option: OptionItem): string | null {
        return this.attributes?.['icon'] ?? this.widget?.definition?.icon ?? option.icon;
    }

    private async resolveAction(node: Node): Promise<ResolvedAction | null> {
        const optionName = this.attributes?.['option'];
        if (!node || !optionName || !this.optionsHelper) {
            return null;
        }
        // the options helper applies constrains, permissions, toolpermissions, scopes and the
        // show callbacks, so an option missing from this list must not be offered
        const options = await this.optionsHelper.getAvailableOptions(Target.Actionbar, [node]);
        const option = options?.find((available) => available.name === optionName);
        return option ? { option, node } : null;
    }
}
