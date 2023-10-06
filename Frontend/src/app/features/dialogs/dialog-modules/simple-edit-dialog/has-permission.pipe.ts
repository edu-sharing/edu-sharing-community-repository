import { Pipe, PipeTransform } from '@angular/core';

import { Node } from 'ngx-edu-sharing-api';

@Pipe({
    name: 'hasPermission',
})
export class HasPermissionPipe implements PipeTransform {
    transform(nodes: Node[], permission: string): boolean {
        return nodes?.every((node) => node.access.includes(permission));
    }
}
