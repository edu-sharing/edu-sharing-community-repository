import { PipeTransform, Pipe } from '@angular/core';
import { Node, RestConstants } from 'ngx-edu-sharing-api';

@Pipe({ name: 'NodeImageSize' })
export class NodeImageSizePipe implements PipeTransform {
    transform(node: Node, args: any = null): string {
        const width = parseFloat(node.properties[RestConstants.CCM_PROP_WIDTH]?.[0]);
        const height = parseFloat(node.properties[RestConstants.CCM_PROP_HEIGHT]?.[0]);
        const megapixel = Math.round((width * height) / 1000000);
        if (width && height) {
            if (megapixel > 1) {
                return megapixel + ' Megapixel';
            }
            return Math.round(width) + 'x' + Math.round(height);
        }
        return '';
    }
}
