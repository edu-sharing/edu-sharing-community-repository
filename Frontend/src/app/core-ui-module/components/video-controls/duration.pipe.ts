import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'duration',
})
export class DurationPipe implements PipeTransform {
    transform(value: number, maxValue: number = value): string {
        const totalSeconds = Math.floor(value);
        const hours: number = Math.floor(totalSeconds / 3600);
        const minutes: number = Math.floor((totalSeconds - hours * 3600) / 60);
        const seconds: number = totalSeconds - hours * 3600 - minutes * 60;
        if (maxValue < 3600) {
            return toTwoDigitString(minutes) + ':' + toTwoDigitString(seconds);
        } else {
            return (
                toTwoDigitString(hours) +
                ':' +
                toTwoDigitString(minutes) +
                ':' +
                toTwoDigitString(seconds)
            );
        }
    }
}

function toTwoDigitString(n: number): string {
    if (n < 10) {
        return '0' + n.toString();
    } else {
        return n.toString();
    }
}
