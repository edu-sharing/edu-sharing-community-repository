import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ListItem } from '../../core-module/core.module';

@Pipe({ name: 'appListItemLabel' })
export class ListItemLabelPipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(item: ListItem, args = { fallback: item.name }) {
        const mapping = {
            NODE: 'NODE',
            COLLECTION: 'NODE',
            NODE_PROPOSAL: 'NODE_PROPOSAL',
            ORG: 'ORG',
            GROUP: 'GROUP',
            USER: 'USER',
        };
        return (
            item.label ||
            this.translate.instant(mapping[item.type] + '.' + item.name, {
                fallback: args.fallback,
            })
        );
    }
}
