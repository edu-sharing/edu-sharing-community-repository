import { DurationFormat, DurationHelper } from './duration-helper';

describe('testing convert duration', () => {
    it('converting hh:mm:ss', () => {
        expect(DurationHelper.getDurationInSeconds(null)).toEqual(0);
        expect(DurationHelper.getDurationInSeconds('')).toEqual(0);
        expect(DurationHelper.getDurationInSeconds('00:01:00')).toEqual(60);
        expect(DurationHelper.getDurationInSeconds('01:01:01')).toEqual(3600 + 60 + 1);
    });
    it('converting mm:ss', () => {
        expect(DurationHelper.getDurationInSeconds('00:01')).toEqual(1);
        expect(DurationHelper.getDurationInSeconds('01:01')).toEqual(60 + 1);
    });
    it('converting PT', () => {
        expect(DurationHelper.getDurationInSeconds('PT1H1M1S')).toEqual(3600 + 60 + 1);
        expect(DurationHelper.getDurationInSeconds('PT1M1S')).toEqual(60 + 1);
        expect(DurationHelper.getDurationInSeconds('PT1S')).toEqual(1);
    });
});
describe('testing format duration', () => {
    it('formatting colon', () => {
        expect(DurationHelper.getDurationFormatted('0:0:1')).toEqual('00:00:01');
        expect(DurationHelper.getDurationFormatted('0:1:0')).toEqual('00:01:00');
        expect(DurationHelper.getDurationFormatted('1:1:1')).toEqual('01:01:01');
    });
    it('formatting Hms', () => {
        expect(DurationHelper.getDurationFormatted('0:0:1', DurationFormat.Hms)).toEqual('1s');
        expect(DurationHelper.getDurationFormatted('0:1:0', DurationFormat.Hms)).toEqual('1m');
        expect(DurationHelper.getDurationFormatted('1:1:1', DurationFormat.Hms)).toEqual(
            '1h 1m 1s',
        );
        expect(DurationHelper.getDurationFormatted('1:0:0', DurationFormat.Hms)).toEqual('1h');
        expect(DurationHelper.getDurationFormatted('0:0:0', DurationFormat.Hms)).toEqual('');
    });
});
