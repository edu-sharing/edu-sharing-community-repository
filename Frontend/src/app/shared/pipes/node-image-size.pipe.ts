import {PipeTransform, Pipe} from '@angular/core';
import {RestConstants} from '../../core-module/rest/rest-constants';
import {Node} from '../../core-module/rest/data-object';

@Pipe({name: 'NodeImageSize'})
export class NodeImageSizePipe implements PipeTransform {
  transform(node : Node,args:any = null): string {
    const width=node.properties[RestConstants.CCM_PROP_WIDTH];
    const height=node.properties[RestConstants.CCM_PROP_HEIGHT];
    const megapixel=Math.round((width*height)/1000000.);
    if(width && height) {
      if(megapixel>1) {
        return megapixel+' Megapixel';
      }
      return Math.round(width) + 'x' + Math.round(height);
    }
    return '';
  }
}
