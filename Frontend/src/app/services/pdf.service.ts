import { Injectable, Injector } from '@angular/core';
import pdfMake from 'pdfmake/build/pdfmake';
import pdfFonts from 'pdfmake/build/vfs_fonts';
import { Content, ContentText, TDocumentDefinitions } from 'pdfmake/interfaces';
import { Node } from 'ngx-edu-sharing-api';
import { RestConstants } from '../core-module/rest/rest-constants';
import { NodeHelperService } from './node-helper.service';
import QRCode from 'qrcode';
import { TranslateService } from '@ngx-translate/core';
import { MdsEditorInstanceService } from '../features/mds/mds-editor/mds-editor-instance.service';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ColorHelper, MdsViewerService, NodeLicensePipe } from 'ngx-edu-sharing-ui';
import { filter } from 'rxjs/operators';

pdfMake.vfs = pdfFonts.vfs;

@Injectable()
export class PdfService {
    constructor(
        private injector: Injector,
        private translate: TranslateService,
        private mdsEditorInstanceService: MdsEditorInstanceService,
        private mdsViewerService: MdsViewerService,
        private http: HttpClient,
    ) {}

    public async triggerMetaDataPdfDownload(node: Node): Promise<void> {
        const title =
            node.properties[RestConstants.LOM_PROP_TITLE]?.[0] ||
            node.properties[RestConstants.CM_NAME]?.[0];

        let imageData = null;
        let imageDimensions = null;
        if (node.preview?.url) {
            try {
                const helper = this.injector.get(NodeHelperService);
                await helper.appendImageData(node, 70, 400);
                imageData = node.preview.data;
                imageDimensions = await this.getImageDimensions(imageData, 100);
            } catch (error) {
                console.warn('Could not load preview image:', error);
            }
        }

        const logoSvg = await this.loadSvgContent('assets/images/logo.svg');

        const content: Content = [];

        if (logoSvg) {
            content.push({
                columns: [
                    {
                        text: title,
                        fontSize: 18,
                        bold: true,
                        alignment: 'left',
                        margin: [0, 10, 0, 0],
                    },
                    {
                        svg: logoSvg,
                        width: 100,
                        height: 40,
                        alignment: 'right',
                    },
                ],
                margin: [0, 0, 0, 20],
            });
        } else {
            content.push({ text: title, fontSize: 18, bold: true, margin: [0, 20, 0, 0] });
        }

        content.push({
            canvas: [
                {
                    type: 'line',
                    x1: 0,
                    y1: 0,
                    x2: 515,
                    y2: 0,
                    lineWidth: 1,
                    lineColor: getComputedStyle(document.documentElement).getPropertyValue(
                        '--tableSeperatorLineColor',
                    ),
                },
            ],
            margin: [0, 10, 0, 10],
        });

        const urlBlock = {
            text: this.getNodeUrl(node),
            link: this.getNodeUrl(node),
            columnGap: 20,
            color: getComputedStyle(document.documentElement).getPropertyValue('--primary'),
            margin: [0, 10, 0, 10],
            decoration: 'underline',
            fontSize: 10,
            wordBreak: 'break-all',
            noWrap: false,
        } as ContentText;

        const qrCodeData = await this.createQrCode(node);
        if (imageData && qrCodeData && imageDimensions) {
            content.push({
                columns: [
                    {
                        image: await this.addRoundedCornersToImage(imageData),
                        width: imageDimensions.width,
                        height: imageDimensions.height,
                    },
                    { width: '*', text: '' },
                    {
                        width: '200',
                        alignment: 'right',
                        stack: [
                            {
                                image: qrCodeData,
                                width: 100,
                            },
                            urlBlock,
                        ],
                    },
                ],
                columnGap: 20,
                margin: [0, 10, 0, 10],
            });
        } else if (imageData && imageDimensions) {
            content.push({
                image: await this.addRoundedCornersToImage(imageData),
                width: imageDimensions.width,
                height: imageDimensions.height,
                alignment: 'left',
                margin: [0, 10, 0, 10],
            });
        } else if (qrCodeData) {
            content.push({
                image: qrCodeData,
                height: 100,
                alignment: 'left',
                margin: [0, 10, 0, 10],
            });
        }

        content.push(
            {
                canvas: [
                    {
                        type: 'line',
                        x1: 0,
                        y1: 0,
                        x2: 515,
                        y2: 0,
                        lineWidth: 1,
                        lineColor: getComputedStyle(document.documentElement).getPropertyValue(
                            '--tableSeperatorLineColor',
                        ),
                    },
                ],
                margin: [0, 10, 0, 10],
            },
            {
                text: '\n' + (await firstValueFrom(this.translate.get('OPTIONS.METADATA'))),
                fontSize: 14,
                margin: [0, 0, 0, 10],
            },
            {
                table: {
                    widths: ['35%', '65%'],
                    body: await this.getMetadataTableContent(node),
                },
                layout: {
                    fillColor: (rowIndex: any, node: any, columnIndex: number) =>
                        columnIndex === 0
                            ? ColorHelper.rgbToHex(
                                  ColorHelper.cssColorToRgb(
                                      getComputedStyle(document.documentElement).getPropertyValue(
                                          '--palette-primary-50',
                                      ),
                                  ),
                              )
                            : null,
                    hLineWidth: () => 0.5,
                    vLineWidth: () => 0,
                    hLineColor: () =>
                        getComputedStyle(document.documentElement).getPropertyValue(
                            '--tableSeperatorLineColor',
                        ),
                    paddingLeft: () => 10,
                    paddingRight: () => 10,
                    paddingTop: () => 6,
                    paddingBottom: () => 6,
                },
                fontSize: 10,
            },
        );

        const documentDefinition: TDocumentDefinitions = {
            content,
        };

        pdfMake
            .createPdf(documentDefinition)
            .download(
                (this.injector.get(NodeHelperService).getFilenameWithoutExtension(node.name) ||
                    node.ref.id) + '.pdf',
            );
    }

    private async createQrCode(node: Node): Promise<string> {
        try {
            return await QRCode.toDataURL(this.getNodeUrl(node), {
                errorCorrectionLevel: 'M',
                type: 'image/jpeg',
                margin: 2,
                width: 200,
            });
        } catch (error) {
            console.error('Error generating QR code:', error);
            return '';
        }
    }

    private getNodeUrl(node: Node) {
        return this.injector.get(NodeHelperService).getNodeUrl(node, null, true);
    }

    private async addRoundedCornersToImage(
        base64Image: string,
        cornerRadius: number = 10,
    ): Promise<string> {
        return new Promise((resolve) => {
            const img = new Image();
            img.onload = () => {
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                canvas.width = img.width;
                canvas.height = img.height;
                ctx.beginPath();
                ctx.roundRect(0, 0, canvas.width, canvas.height, cornerRadius);
                ctx.clip();
                ctx.drawImage(img, 0, 0);
                resolve(canvas.toDataURL('image/png'));
            };
            img.src = base64Image;
        });
    }

    private async getImageDimensions(
        base64Image: string,
        targetHeight: number,
    ): Promise<{ width: number; height: number }> {
        return new Promise((resolve) => {
            const img = new Image();
            img.onload = () => {
                const aspectRatio = img.width / img.height;
                const calculatedWidth = targetHeight * aspectRatio;
                resolve({
                    width: calculatedWidth,
                    height: targetHeight,
                });
            };
            img.onerror = () => {
                resolve({ width: targetHeight, height: targetHeight });
            };
            img.src = base64Image;
        });
    }

    private async getMetadataTableContent(node: Node): Promise<string[][]> {
        const content = [];
        await this.mdsEditorInstanceService.initWithNodes([node], { groupId: 'io_text_pdf' });
        const widgets = this.mdsEditorInstanceService.widgets.value;
        for (const widget of widgets) {
            if (widget.getValue && widget.getValue().length > 0) {
                const initialValues = await widget.getInitalValuesAsync();
                const displayValues = await firstValueFrom(
                    widget.getInitialDisplayValues().pipe(filter((v) => !!v)),
                );
                content.push([
                    widget.definition.caption,
                    this.mdsViewerService.getFormattedValue(
                        displayValues.values?.map((v) => v.displayString || v.key) ||
                            initialValues.jointValues,
                        widget.definition,
                        MdsViewerService.getBasicType(widget.definition, true),
                    )[0],
                ]);
            }
        }
        content.push(await this.getLicenseRow(node));
        return content;
    }

    private async loadSvgContent(svgPath: string): Promise<string> {
        try {
            return await firstValueFrom(this.http.get(svgPath, { responseType: 'text' }));
        } catch (error) {
            console.error('Error loading SVG:', error);
            return '';
        }
    }

    private async getLicenseRow(node: Node): Promise<string[]> {
        return [
            await firstValueFrom(this.translate.get('MDS.LICENSE')),
            await firstValueFrom(
                new NodeLicensePipe(this.translate).transform(node, { type: 'name' }),
            ),
        ];
    }
}
