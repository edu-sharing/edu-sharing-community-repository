import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'htmlTextPipe',
})
export class HtmlTextPipe implements PipeTransform {
    transform(value: string): string {
        const temp = document.createElement('div');
        temp.innerHTML = value;
        return temp.textContent || temp.innerText || '';
    }
}
