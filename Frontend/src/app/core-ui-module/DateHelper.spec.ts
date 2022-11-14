import { DateHelper, FormatOptions, RelativeMode } from './DateHelper';
import { TranslateService } from '@ngx-translate/core/lib/translate.service';

const translateServiceMock = {
    instant: (key: string, values?: any) => key + JSON.stringify(values),
} as TranslateService;
describe('testing relative dates', () => {
    const date_now = new Date(new Date().getTime());
    const date_10_mins_before = new Date(new Date().getTime() - 1000 - 10 * 60 * 1000);
    const date_4_hours_before = new Date(new Date().getTime() - 1000 - 4 * 3600 * 1000);
    const date_1_day_before = new Date(new Date().getTime() - 1000 - 24 * 3600 * 1000);
    const date_2_days_before = new Date(new Date().getTime() - 1000 - 2 * 24 * 3600 * 1000);
    const date_7_days_before = new Date(new Date().getTime() - 1000 - 7 * 24 * 3600 * 1000);
    const date_2_months_before = new Date();
    date_2_months_before.setMonth(date_2_months_before.getMonth() - 2);
    const date_1_year_before = new Date();
    date_1_year_before.setFullYear(date_1_year_before.getFullYear() - 1);
    for (const mode of [
        RelativeMode.short,
        RelativeMode.medium,
        RelativeMode.large,
        RelativeMode.all,
    ]) {
        it('testing relative mode ' + mode, () => {
            const options: FormatOptions = {
                relativeLabels: mode as any,
                showAlwaysTime: false,
            };
            expect(DateHelper.formatDate(translateServiceMock, date_now, options)).toContain(
                'JUST_NOW',
            );
            expect(
                DateHelper.formatDate(translateServiceMock, date_4_hours_before, options),
            ).toContain('HOURS_AGO');
            expect(
                DateHelper.formatDate(translateServiceMock, date_4_hours_before, options),
            ).toContain('4');
            expect(
                DateHelper.formatDate(translateServiceMock, date_10_mins_before, options),
            ).toContain('MINUTES_AGO');
            expect(
                DateHelper.formatDate(translateServiceMock, date_10_mins_before, options),
            ).toContain('10');
            if (mode === RelativeMode.short) {
                expect(
                    DateHelper.formatDate(translateServiceMock, date_1_day_before, options),
                ).not.toContain('YESTERDAY');
                expect(
                    DateHelper.formatDate(translateServiceMock, date_2_days_before, options),
                ).not.toBe('DAYS_AGO');
                expect(
                    DateHelper.formatDate(translateServiceMock, date_7_days_before, options),
                ).not.toBe('DAYS_AGO');
            } else {
                expect(
                    DateHelper.formatDate(translateServiceMock, date_1_day_before, options),
                ).toContain('YESTERDAY');
                expect(
                    DateHelper.formatDate(translateServiceMock, date_2_days_before, options),
                ).toContain('DAYS_AGO');
                expect(
                    DateHelper.formatDate(translateServiceMock, date_2_days_before, options),
                ).toContain('2');
                if (mode === RelativeMode.medium) {
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_7_days_before, options),
                    ).not.toContain('DAYS_AGO');
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_2_months_before, options),
                    ).not.toContain('MONTHS_AGO');
                } else {
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_7_days_before, options),
                    ).toContain('DAYS_AGO');
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_7_days_before, options),
                    ).toContain('7');
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_2_months_before, options),
                    ).toContain('MONTHS_AGO');
                    expect(
                        DateHelper.formatDate(translateServiceMock, date_2_months_before, options),
                    ).toContain('2');
                    if (mode == RelativeMode.large) {
                        expect(
                            DateHelper.formatDate(
                                translateServiceMock,
                                date_1_year_before,
                                options,
                            ),
                        ).not.toContain('YEARS_AGO');
                    } else {
                        expect(
                            DateHelper.formatDate(
                                translateServiceMock,
                                date_1_year_before,
                                options,
                            ),
                        ).toContain('YEARS_AGO');
                        expect(
                            DateHelper.formatDate(
                                translateServiceMock,
                                date_1_year_before,
                                options,
                            ),
                        ).toContain('1');
                    }
                }
            }
        });
    }
});
