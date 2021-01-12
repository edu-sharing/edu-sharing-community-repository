import { PipeTransform, Pipe } from '@angular/core';

@Pipe({name: 'keys'})
export class KeysPipe implements PipeTransform {
    transform(value:any, args:string[]) : any {
        let keys = [];
        for (let key in value) {
            keys.push(key);
        }
        return keys;
    }
}