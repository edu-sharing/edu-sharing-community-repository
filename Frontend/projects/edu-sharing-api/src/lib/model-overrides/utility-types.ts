/**
 * Narrow `P` with `T` if `T` extends `P`, otherwise use `P` without regarding `T`.
 *
 * Applying this to properties of an API model lets us narrow down property types but ensures that
 * we don't add or redefine properties which are incompatible with the original model.
 */
export type Narrow<P, T> = T extends P ? T : P;
