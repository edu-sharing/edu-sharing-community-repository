/**
 * Copy paste from rxjs/util/isNumeric since importing it from there will trigger a warning about
 * CommonJS or AMD dependencies and indeed increase the bundle size a bit.
 */

// From https://github.com/ReactiveX/rxjs/blob/6.x/src/internal/util/isArray.ts
const isArray = (() =>
    Array.isArray || (<T>(x: any): x is T[] => x && typeof x.length === 'number'))();

// From https://github.com/ReactiveX/rxjs/blob/6.x/src/internal/util/isNumeric.ts
export function isNumeric(val: any): val is number | string {
    return !isArray(val) && val - parseFloat(val) + 1 >= 0;
}
