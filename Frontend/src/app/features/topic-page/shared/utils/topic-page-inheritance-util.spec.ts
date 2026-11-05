import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { findInheritanceSource, resolveInheritance } from './topic-page-inheritance-util';

const collection = (
    id: string,
    { propagates = false, stops = false, ownPage = false } = {},
): Node =>
    ({
        ref: { id },
        properties: {
            ...(propagates && {
                [RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_REF]: [
                    `workspace://SpacesStore/${id}`,
                ],
            }),
            ...(stops && { [RestConstants.CCM_PROP_PAGE_CONFIG_PROPAGATE_STOP]: ['true'] }),
            ...(ownPage && { [RestConstants.CCM_PROP_PAGE_CONFIG_REF]: ['workspace://x/page'] }),
        },
    } as unknown as Node);

describe('findInheritanceSource', () => {
    const wlo = collection('wlo', { propagates: true });

    it('finds the nearest ancestor passing templates on', () => {
        const physics = collection('physics');
        const optics = collection('optics');
        expect(findInheritanceSource(optics, [optics, physics, wlo])).toBe(wlo);
    });

    it('lets a collection that stops passing on still inherit itself', () => {
        const optics = collection('optics', { stops: true });
        expect(findInheritanceSource(optics, [optics, collection('physics'), wlo])).toBe(wlo);
    });

    it('stops below a collection that does not pass inherited pages on', () => {
        const colors = collection('colors');
        const optics = collection('optics', { stops: true });
        expect(findInheritanceSource(colors, [optics, collection('physics'), wlo])).toBeNull();
    });

    it('passes the templates of a stopping collection itself on', () => {
        const colors = collection('colors');
        const optics = collection('optics', { stops: true, propagates: true });
        expect(findInheritanceSource(colors, [optics, wlo])).toBe(optics);
    });
});

describe('resolveInheritance', () => {
    const wlo = collection('wlo', { propagates: true });

    it('reports where inherited templates were stopped', () => {
        const optics = collection('optics', { stops: true });
        expect(resolveInheritance(collection('colors'), [optics, wlo])).toEqual({
            source: null,
            stoppedAt: optics,
        });
    });

    it('reports nothing stopped when no ancestor passes templates on', () => {
        const optics = collection('optics', { stops: true });
        expect(resolveInheritance(collection('colors'), [optics])).toEqual({
            source: null,
            stoppedAt: null,
        });
    });
});
