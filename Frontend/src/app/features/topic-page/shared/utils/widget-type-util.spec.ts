import { WIDGETS } from '../types/custom-definitions';
import { canRenderWidgetType, WidgetTypeSupport } from './widget-type-util';

describe('canRenderWidgetType', () => {
    const fullSupport: WidgetTypeSupport = {
        aiSupported: true,
        rendering2Supported: true,
        hasCustomWidget: () => false,
    };

    it('accepts built-in widget types', () => {
        expect(canRenderWidgetType(WIDGETS.CONTENT_TEASER, fullSupport)).toBe(true);
        expect(canRenderWidgetType(WIDGETS.COLLECTION_CHIPS, fullSupport)).toBe(true);
    });

    it('rejects the AI text widget without AI support', () => {
        expect(canRenderWidgetType(WIDGETS.AI_TEXT_WIDGET, fullSupport)).toBe(true);
        expect(
            canRenderWidgetType(WIDGETS.AI_TEXT_WIDGET, { ...fullSupport, aiSupported: false }),
        ).toBe(false);
    });

    it('rejects media rendering without rendering2 support', () => {
        expect(canRenderWidgetType(WIDGETS.MEDIA_RENDERING, fullSupport)).toBe(true);
        expect(
            canRenderWidgetType(WIDGETS.MEDIA_RENDERING, {
                ...fullSupport,
                rendering2Supported: false,
            }),
        ).toBe(false);
    });

    it('accepts registered custom widgets and rejects unknown types', () => {
        const support: WidgetTypeSupport = {
            ...fullSupport,
            hasCustomWidget: (widgetType: string) => widgetType === 'custom-widget',
        };
        expect(canRenderWidgetType('custom-widget', support)).toBe(true);
        expect(canRenderWidgetType('unknown-widget', support)).toBe(false);
    });
});
