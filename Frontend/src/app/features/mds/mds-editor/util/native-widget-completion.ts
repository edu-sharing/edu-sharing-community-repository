import { VCard } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { NativeWidgetType, Values } from '../../types/types';

/**
 * Emptiness predicates for native widgets that can participate in the completion status while not
 * being rendered as components (e.g. the when using observeCompletionStatus() and never
 * instantiates the native widget components).
 *
 * This module intentionally imports only leaf dependencies (no component classes, no
 * `MdsEditorInstanceService`, no `mds-types` registry) so it can be imported by both the native
 * widget components AND `MdsEditorInstanceService` without creating a circular import — which is
 * what leaves the `NativeWidgets` registry entries `undefined` at runtime.
 */

/**
 * Single source of truth for "is the author value empty": empty unless the freetext has non-blank
 * content or the person VCard is valid. Used both by the live component state and the
 * {@link nativeWidgetEmptyCheckers} `Values`-based check below.
 */
export function authorIsEmpty(freetext: string, vcard: VCard): boolean {
    return !(freetext?.trim() || vcard?.isValid());
}

/**
 * Derive emptiness for a native widget type directly from the editor `Values` (a node's
 * `properties` map). Only native widgets that can be marked required need an entry here.
 */
export const nativeWidgetEmptyCheckers: Partial<
    Record<NativeWidgetType, (values: Values) => boolean>
> = {
    [NativeWidgetType.Author]: (values) => {
        const vcard = values?.[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0];
        return authorIsEmpty(
            values?.[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0],
            vcard ? new VCard(vcard) : undefined,
        );
    },
};
