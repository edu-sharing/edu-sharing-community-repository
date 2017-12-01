import {PipeTransform, Pipe} from '@angular/core';
import {TranslateService} from "ng2-translate";
import { DomSanitizer } from '@angular/platform-browser';
import {RestConstants} from "../rest/rest-constants";
import {Node} from "../rest/data-object";
import {NodeHelper} from "./node-helper";
import {ListItem} from "./list-item";


@Pipe({name: 'attributeHtml',pure: false})
export class AttributeHtmlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer, private translateService : TranslateService) {}
  transform(text : string, node : Node, item : ListItem ) {
    if (item.type == 'NODE') {
      if (item.name == RestConstants.CM_MODIFIED_DATE)
        return ('<span property="dateModified" title="' + this.translateService.instant('ACCESSIBILITY.LASTMODIFIED') + '">' + text + '</span>');

      if (item.name == RestConstants.CM_CREATOR)
        return ('<span property="author" title="' + this.translateService.instant('ACCESSIBILITY.AUTHOR') + '">' + text + '</span>');

      if (item.name == RestConstants.CCM_PROP_LICENSE) {
        if (node.licenseURL) {
          return this.sanitizer.bypassSecurityTrustHtml('<img src="'+NodeHelper.getLicenseIcon(node)+'" height="20" property="" title="'+NodeHelper.getLicenseName(node,this.translateService)+'">');
        }
        return '';
      }
      if (item.name == RestConstants.CCM_PROP_REPLICATIONSOURCE)
        return this.sanitizer.bypassSecurityTrustHtml('<img src="https://www.kinvey.com/wp-content/uploads/2013/09/may-the-source-be-with-you_open-source.jpg" height="20" property="" title="">');

    }
    return text;
  }
}
