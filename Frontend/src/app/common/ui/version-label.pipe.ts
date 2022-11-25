import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DateHelper } from '../../core-ui-module/DateHelper';
import { isNumeric } from 'rxjs/util/isNumeric';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { Version } from '../../core-module/rest/data-object';

/**
 * Format the version label and checking constants if required
 */
@Pipe({ name: 'versionComment' })
export class VersionLabelPipe implements PipeTransform {
    transform(node: Node | Version | any, args: any = null): string {
        let comment: string;
        if (node.properties?.[RestConstants.CCM_PROP_LIFECYCLE_VERSION_COMMENT]?.[0]) {
            comment = node.properties?.[RestConstants.CCM_PROP_LIFECYCLE_VERSION_COMMENT]?.[0];
        } else {
            comment = node.comment;
        }
        if (comment) {
            if (
                comment === RestConstants.COMMENT_MAIN_FILE_UPLOAD ||
                comment === RestConstants.COMMENT_METADATA_UPDATE ||
                comment === RestConstants.COMMENT_CONTRIBUTOR_UPDATE ||
                comment === RestConstants.COMMENT_CONTENT_UPDATE ||
                comment === RestConstants.COMMENT_LICENSE_UPDATE ||
                comment === RestConstants.COMMENT_NODE_PUBLISHED ||
                comment === RestConstants.COMMENT_PREVIEW_CHANGED ||
                comment === RestConstants.COMMENT_BULK_CREATE ||
                comment === RestConstants.COMMENT_BULK_UPDATE ||
                comment === RestConstants.COMMENT_BULK_UPDATE_RESYNC ||
                comment === RestConstants.COMMENT_REMOTE_OBJECT_INIT ||
                comment === RestConstants.COMMENT_MIGRATION ||
                comment === RestConstants.COMMENT_BLOCKED_IMPORT ||
                comment.startsWith(RestConstants.COMMENT_EDITOR_UPLOAD)
            ) {
                const parameters = comment.split(',');
                let editor = '';
                if (parameters.length > 1)
                    editor = this.translate.instant('CONNECTOR.' + parameters[1] + '.NAME');
                comment = this.translate.instant('WORKSPACE.METADATA.COMMENT.' + parameters[0], {
                    editor,
                });
            }
        } else {
            comment = this.translate.instant('WORKSPACE.METADATA.COMMENT.NONE');
        }
        return comment;
    }
    constructor(private translate: TranslateService) {}
}
