import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'appMdsDuration',
})
export class MdsDurationPipe implements PipeTransform {
    transform(value: string): string {
        if(isNaN(Number(value)) || !value) {
            return '-';
        }
        const totalSeconds = Math.floor(parseInt(value) / 1000);
        const hours: number = Math.floor(totalSeconds / 3600);
        const minutes: number = Math.round((totalSeconds - hours * 3600) / 60);
        if(hours > 0){
            return hours + 'h ' + minutes + 'm';
        }
        return minutes + 'm';

    }
}
