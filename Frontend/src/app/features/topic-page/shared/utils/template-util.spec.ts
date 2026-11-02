import { Node } from 'ngx-edu-sharing-api';
import {
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP,
} from '../types/custom-definitions';
import { isTemplateUpdateAvailable } from './template-util';

const node = (version?: string, isTemplate: boolean = false): Node =>
    ({
        properties: {
            ...(version && { [DEFAULT_PAGE_VARIANT_TEMPLATE_VERSION_PROP]: [version] }),
            ...(isTemplate && { [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: ['true'] }),
        },
    } as unknown as Node);

describe('isTemplateUpdateAvailable', () => {
    it('is false without a template or variant', () => {
        expect(isTemplateUpdateAvailable(null, node('1'))).toBeFalse();
        expect(isTemplateUpdateAvailable(node('1'), null)).toBeFalse();
    });

    it('compares the full version of a page variant', () => {
        expect(isTemplateUpdateAvailable(node('3'), node('3'))).toBeFalse();
        expect(isTemplateUpdateAvailable(node('4'), node('3'))).toBeTrue();
    });

    it('treats a missing version as the default one', () => {
        expect(isTemplateUpdateAvailable(node(), node('1'))).toBeFalse();
    });

    it('compares only the parent part of a non-root template version', () => {
        expect(isTemplateUpdateAvailable(node('3'), node('3:5', true))).toBeFalse();
        expect(isTemplateUpdateAvailable(node('4'), node('3:5', true))).toBeTrue();
        expect(isTemplateUpdateAvailable(node('2:1'), node('2:1:7', true))).toBeFalse();
    });
});
