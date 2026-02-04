import { MdsExtendedValue } from 'ngx-edu-sharing-ui';

export function mapExtendedValues(v: string[] | MdsExtendedValue) {
    return v && !Array.isArray(v) ? Object.keys(v) : (v as string[]);
}
