import { PipeTransform, Pipe } from '@angular/core';
/**
 * Bitwise check for a flag, returns true or false if the flag matches
 * Examples:
 * 1 | bitwise:{operator: 1} === true
 * 3 | bitwise:{operator: 1} === true
 * 4 | bitwise:{operator: 1} === false
 */
@Pipe({ name: 'bitwise' })
export class BitwisePipe implements PipeTransform {
    transform(value: number, args: { operator: number }): boolean {
        // tslint:disable-next-line:no-bitwise
        return (value & args.operator) === args.operator;
    }
}
