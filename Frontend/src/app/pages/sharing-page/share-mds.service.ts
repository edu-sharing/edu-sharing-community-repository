import { Injectable, inject } from '@angular/core';
import { MdsDefinition, MdsIdentifier, MdsService, MetadataSetInfo } from 'ngx-edu-sharing-api';
import { Observable, of } from 'rxjs';
import { RestSharingService } from '../../core-module/core.module';

/**
 * What identifies the content that is currently viewed via a link share.
 *
 * Provided by the sharing page, so everything below it (e.g. the metadata editor) can reach the
 * content without a login.
 */
@Injectable()
export class ShareAccessService {
    nodeId: string = null;
    token: string = null;
    password = '';
    /** the element of a shared folder that is viewed, null for the shared node itself */
    childId: string = null;
}

/**
 * Loads the metadata set through the share instead of the metadata set api, which requires a login.
 *
 * Provided in place of the {@link MdsService} on the sharing page. The set of the viewed node is
 * returned, whatever set the caller asked for.
 */
@Injectable()
export class ShareMdsService extends MdsService {
    private access = inject(ShareAccessService);
    private sharingService = inject(RestSharingService);

    override getMetadataSet(_: Partial<MdsIdentifier>): Observable<MdsDefinition> {
        return this.sharingService.getMetadataSet(
            this.access.nodeId,
            this.access.token,
            this.access.password,
            this.access.childId,
        );
    }

    // there is no list of sets to choose from for a recipient of a share
    override getAvailableMetadataSets(): Observable<MetadataSetInfo[] | null> {
        return of([]);
    }
}
