import { WIDGET_TYPE_OPTIONS, WIDGETS } from '../types/custom-definitions';

/**
 * Installation features the rendering of some widget types depends on.
 */
export interface WidgetTypeSupport {
    aiSupported: boolean;
    rendering2Supported: boolean;
    hasCustomWidget: (widgetType: string) => boolean;
}

/**
 * Whether a topic page can render the given widget type: a built-in type whose feature this
 * installation supports, or a registered custom widget.
 */
export const canRenderWidgetType = (widgetType: string, support: WidgetTypeSupport): boolean => {
    if (widgetType === WIDGETS.AI_TEXT_WIDGET && !support.aiSupported) {
        return false;
    }
    if (widgetType === WIDGETS.MEDIA_RENDERING && !support.rendering2Supported) {
        return false;
    }
    return (
        WIDGET_TYPE_OPTIONS.some((option) => option.value === widgetType) ||
        support.hasCustomWidget(widgetType)
    );
};
