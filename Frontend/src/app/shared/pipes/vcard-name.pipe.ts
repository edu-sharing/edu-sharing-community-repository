import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { VCard } from '../../core-module/ui/VCard';

@Pipe({ name: 'vcardName' })
export class VCardNamePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(authority: string, args: string[] = null): string {
        return authority ? new VCard(authority).getDisplayName() : '';
    }
}
