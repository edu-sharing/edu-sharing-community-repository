import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PdfService } from '../../services/pdf.service';
import { MdsEditorInstanceService } from '../../features/mds/mds-editor/mds-editor-instance.service';
import { Location } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { NodeService } from 'ngx-edu-sharing-api';
import { MdsViewerService } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-pdf-metadata-page',
    template: '',
    providers: [PdfService, MdsEditorInstanceService, MdsViewerService],
    standalone: false,
})
export class PdfMetadataPageComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private location = inject(Location);
    private nodeService = inject(NodeService);
    private pdfService = inject(PdfService);

    async ngOnInit() {
        const nodeId = this.route.snapshot.paramMap.get('nodeId');
        if (nodeId) {
            try {
                const node = await firstValueFrom(this.nodeService.getNode(nodeId));
                await this.pdfService.triggerMetaDataPdfDownload(node);
            } catch (error) {
                console.error('Error generating PDF:', error);
            }
        }

        this.location.back();
    }
}
