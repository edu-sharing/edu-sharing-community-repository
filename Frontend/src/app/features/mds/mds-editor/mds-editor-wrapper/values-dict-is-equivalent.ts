import { Values } from '../../types/types';

export function valuesDictIsEquivalent(lhs: Values, rhs: Values): boolean {
    lhs ??= {};
    rhs ??= {};
    const keys = Array.from(new Set([...Object.keys(lhs), ...Object.keys(rhs)]));
    return keys.every((key) => valuesArrayIsEquivalent(lhs[key], rhs[key]));
}

function valuesArrayIsEquivalent(lhs: string[] = [], rhs: string[] = []): boolean {
    return lhs.every((value) => rhs.includes(value)) && rhs.every((value) => lhs.includes(value));
}
