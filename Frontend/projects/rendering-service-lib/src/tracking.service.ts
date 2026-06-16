import { Injectable } from '@angular/core';
import {
    EduTrackingControllerService,
    EduTrackingControllerWrapperService,
} from 'ngx-rendering-service-api';

@Injectable({ providedIn: 'root' })
export class TrackingService {
    constructor(
        private trackingControllerService: EduTrackingControllerService,
        private trackingControllerServiceToken: EduTrackingControllerWrapperService,
    ) {}

    trackViewedWithToken(nodeId: string, repoId: string, isWebComponent: boolean, token: string) {
        this.trackingControllerServiceToken
            .trackObjectToken({
                nodeId: nodeId,
                repoId: repoId,
                eventType: isWebComponent ? 'VIEW_EMBEDDED' : 'VIEW_MATERIAL',
                token: token,
            })
            .subscribe();
    }

    trackPlayed(nodeId: string, repoId: string) {
        this.trackingControllerService
            .trackObject({
                body: {
                    nodeId: nodeId,
                    repoId: repoId,
                    eventType: 'VIEW_MATERIAL_PLAY_MEDIA',
                },
            })
            .subscribe();
    }

    trackClicked(nodeId: string, repoId: string) {
        this.trackingControllerService
            .trackObject({
                body: {
                    nodeId: nodeId,
                    repoId: repoId,
                    eventType: 'OPEN_EXTERNAL_LINK',
                },
            })
            .subscribe();
    }

    trackGdprConsent(nodeId: string, repoId: string) {
        this.trackingControllerService
            .trackObject({
                body: {
                    nodeId: nodeId,
                    repoId: repoId,
                    eventType: 'VIEW_MATERIAL_GDPR_CONFIRMED',
                },
            })
            .subscribe();
    }
}
