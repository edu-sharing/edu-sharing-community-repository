import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { VCard } from '../util/VCard';

@Injectable({ providedIn: 'root' })
@Pipe({
    name: 'vcardName',
    standalone: false,
})
export class VCardNamePipe implements PipeTransform {
    transform(authority: string | string[], args: string[] = null): string {
        if (Array.isArray(authority)) {
            return authority
                .map((a) => (a ? new VCard(a).getDisplayName() : ''))
                .filter((s) => !!s)
                .join(', ');
        }
        return authority ? new VCard(authority).getDisplayName() : '';
    }
}
