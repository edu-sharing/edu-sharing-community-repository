import {Pipe, PipeTransform} from '@angular/core';
import {Helper} from '../../core-module/rest/helper';
import {ColorHelper} from '../../core-module/ui/color-helper';
import {NodeHelperService} from "../node-helper.service";
import {RestNetworkService} from "../../core-module/rest/services/rest-network.service";
import {RestConstants} from "../../core-module/rest/rest-constants";
@Pipe({name: 'appNodeSource'})
export class NodeSourcePipe implements PipeTransform {
    constructor(private nodeHelper: NodeHelperService, private networkService: RestNetworkService) {
    }

    transform(replicationSource: string, args: {
        mode: 'text' | 'url' | 'escaped'
    }): string {
      const rawSrc = replicationSource ? replicationSource.toString().trim() : 'home';
      if(args.mode === 'text') {
          if(rawSrc === 'home') {
              return RestNetworkService.getRepositoryById(RestConstants.HOME_REPOSITORY)?.title || 'home';
          }
          return rawSrc;
      } else if(args.mode === 'url') {
          const src = this.escape(rawSrc);
          return this.nodeHelper.getSourceIconPath(src);
      } else if(args.mode === 'escaped') {
          return this.escape(rawSrc);
      }
      return null;
  }

    private escape(src: string) {
        if(!src) {
            return src;
        }
        src = src.substring(src.lastIndexOf(':') + 1).toLowerCase();
        src = src.replace(/\s/g, '_');
        src = src.replace(/\./g, '_');
        src = src.replace(/\//g, '_');
        return src;
    }
}
