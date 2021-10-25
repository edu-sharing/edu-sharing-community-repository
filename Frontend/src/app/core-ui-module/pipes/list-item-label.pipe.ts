import {Pipe, PipeTransform} from '@angular/core';
import {Group, ListItem, Permission, RestConstants, User} from '../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
@Pipe({name: 'appListItemLabel'})
export class ListItemLabelPipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(item: ListItem, args = {fallback: item.name}) {
        return item.label || this.translate.instant(
            (item.type === 'NODE_PROPOSAL' ? 'NODE_PROPOSAL' : 'NODE') + '.' + item.name,
            {fallback: args.fallback}
        );
    };
}

