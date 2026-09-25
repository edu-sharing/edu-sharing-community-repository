import { isSimplePageRoute } from './simple-page';

describe('isSimplePageRoute', () => {
    it('matches a simple page with and without trailing slash, query or hash', () => {
        expect(isSimplePageRoute('components/error/503')).toBeTrue();
        expect(isSimplePageRoute('components/error/503/')).toBeTrue();
        expect(isSimplePageRoute('components/error/503?next=%2F')).toBeTrue();
        expect(isSimplePageRoute('components/error/503#x')).toBeTrue();
    });

    it('does not match the regular app', () => {
        expect(isSimplePageRoute('')).toBeFalse();
        expect(isSimplePageRoute('components/error/500')).toBeFalse();
        expect(isSimplePageRoute('components/error/503/details')).toBeFalse();
        expect(isSimplePageRoute('components/workspace')).toBeFalse();
    });
});
