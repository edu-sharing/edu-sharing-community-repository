import { MdsExtendedValues } from 'ngx-edu-sharing-ui';
import { mapExtendedValues } from './extended-values-mapper';

export function valuesDictIsEquivalent(lhs: MdsExtendedValues, rhs: MdsExtendedValues): boolean {
    lhs ??= {};
    rhs ??= {};
    const keys = Array.from(new Set([...Object.keys(lhs), ...Object.keys(rhs)]));
    return keys.every((key) =>
        valuesArrayIsEquivalent(mapExtendedValues(lhs[key]), mapExtendedValues(rhs[key])),
    );
}

function valuesArrayIsEquivalent(lhs: string[] = [], rhs: string[] = []): boolean {
    return lhs.every((value) => rhs.includes(value)) && rhs.every((value) => lhs.includes(value));
}
