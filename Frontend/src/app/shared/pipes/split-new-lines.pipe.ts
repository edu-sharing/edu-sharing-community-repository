import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'splitNewLines',
})
export class SplitNewLinesPipe implements PipeTransform {
    transform(value: string): string[] {
        return value?.split('\n').filter((line) => !!line);
    }
}
