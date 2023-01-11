import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestConnectorService } from '../../core-module/core.module';
import { Closable, configForNode, configForNodes } from './card-dialog/card-dialog-config';
import { CardDialogRef } from './card-dialog/card-dialog-ref';
import { CardDialogService } from './card-dialog/card-dialog.service';
import {
    AddFolderDialogData,
    AddFolderDialogResult,
} from './dialog-modules/add-folder-dialog/add-folder-dialog-data';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
} from './dialog-modules/contributor-edit-dialog/contributor-edit-dialog-data';
import {
    ContributorsDialogData,
    ContributorsDialogResult,
} from './dialog-modules/contributors-dialog/contributors-dialog-data';
import {
    CreateMapLinkDialogData,
    CreateMapLinkDialogResult,
} from './dialog-modules/create-map-link-dialog/create-map-link-dialog-data';
import {
    FileChooserDialogData,
    FileChooserDialogResult,
} from './dialog-modules/file-chooser-dialog/file-chooser-dialog-data';
import {
    GenericDialogConfig,
    GenericDialogData,
} from './dialog-modules/generic-dialog/generic-dialog-data';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from './dialog-modules/license-agreement-dialog/license-agreement-dialog-data';
import {
    LicenseDialogData,
    LicenseDialogResult,
} from './dialog-modules/license-dialog/license-dialog-data';
import { NodeEmbedDialogData } from './dialog-modules/node-embed-dialog/node-embed-dialog.component';
import { NodeInfoDialogData } from './dialog-modules/node-info-dialog/node-info-dialog.component';
import { NodeReportDialogData } from './dialog-modules/node-report-dialog/node-report-dialog.component';
import { QrDialogData } from './dialog-modules/qr-dialog/qr-dialog.component';
import {
    ShareDialogData,
    ShareDialogResult,
} from './dialog-modules/share-dialog/share-dialog-data';
import {
    ShareHistoryDialogData,
    ShareHistoryDialogResult,
} from './dialog-modules/share-history-dialog/share-history-dialog-data';
import {
    ShareLinkDialogData,
    ShareLinkDialogResult,
} from './dialog-modules/share-link-dialog/share-link-dialog-data';
import {
    XmlAppPropertiesDialogData,
    XmlAppPropertiesDialogResult,
} from './dialog-modules/xml-app-properties-dialog/xml-app-properties-dialog-data';

@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    get openDialogs() {
        return this.cardDialog.openDialogs;
    }

    constructor(
        private cardDialog: CardDialogService,
        private translate: TranslateService,
        // TODO: Move the methods we use of `RestConnectorService` to a utils function if possible.
        private restConnector: RestConnectorService,
    ) {}

    async openGenericDialog<R extends string>(
        config: GenericDialogConfig<R>,
    ): Promise<CardDialogRef<GenericDialogData<R>, R>> {
        const { title, maxWidth, closable, ...data } = {
            ...new GenericDialogConfig<R>(),
            ...config,
        };
        const { GenericDialogComponent } = await import(
            './dialog-modules/generic-dialog/generic-dialog.module'
        );
        return this.cardDialog.open(GenericDialogComponent, {
            title,
            maxWidth,
            closable,
            data,
        });
    }

    async openQrDialog(data: QrDialogData): Promise<CardDialogRef<QrDialogData, void>> {
        const { QrDialogComponent } = await import('./dialog-modules/qr-dialog/qr-dialog.module');
        return this.cardDialog.open(QrDialogComponent, {
            title: 'OPTIONS.QR_CODE',
            ...configForNode(data.node),
            contentPadding: 0,
            data,
        });
    }

    async openNodeEmbedDialog(
        data: NodeEmbedDialogData,
    ): Promise<CardDialogRef<NodeEmbedDialogData, void>> {
        const { NodeEmbedDialogComponent } = await import(
            './dialog-modules/node-embed-dialog/node-embed-dialog.module'
        );
        return this.cardDialog.open(NodeEmbedDialogComponent, {
            title: 'OPTIONS.EMBED',
            ...configForNode(data.node),
            // Set size via NodeEmbedDialogComponent, so it can choose fitting values for its
            // responsive layouts.
            contentPadding: 0,
            data,
        });
    }

    async openNodeReportDialog(
        data: NodeReportDialogData,
    ): Promise<CardDialogRef<NodeReportDialogData, void>> {
        const { NodeReportDialogComponent } = await import(
            './dialog-modules/node-report-dialog/node-report-dialog.module'
        );
        return this.cardDialog.open(NodeReportDialogComponent, {
            title: 'NODE_REPORT.TITLE',
            ...configForNode(data.node),
            data,
        });
    }

    async openNodeInfoDialog(
        data: NodeInfoDialogData,
    ): Promise<CardDialogRef<NodeInfoDialogData, void>> {
        const { NodeInfoComponent } = await import(
            './dialog-modules/node-info-dialog/node-info-dialog.module'
        );
        return this.cardDialog.open(NodeInfoComponent, {
            // Header will be configured by the component
            data,
        });
    }

    async openNodeStoreDialog(): Promise<CardDialogRef<void, void>> {
        const { SearchNodeStoreComponent } = await import(
            './dialog-modules/node-store-dialog/node-store-dialog.module'
        );
        return this.cardDialog.open(SearchNodeStoreComponent, {
            title: 'SEARCH.NODE_STORE.TITLE',
            width: 400,
            minHeight: 600,
            contentPadding: 0,
        });
    }

    async openLicenseAgreementDialog(
        data: LicenseAgreementDialogData,
    ): Promise<CardDialogRef<LicenseAgreementDialogData, LicenseAgreementDialogResult>> {
        const { LicenseAgreementDialogComponent } = await import(
            './dialog-modules/license-agreement-dialog/license-agreement-dialog.module'
        );
        return this.cardDialog.open(LicenseAgreementDialogComponent, {
            title: 'LICENSE_AGREEMENT.TITLE',
            closable: Closable.Disabled,
            width: 900,
            data,
        });
    }

    async openAccessibilityDialog(): Promise<CardDialogRef<void, void>> {
        const { AccessibilityDialogComponent } = await import(
            './dialog-modules/accessibility-dialog/accessibility-dialog.module'
        );
        return this.cardDialog.open(AccessibilityDialogComponent, {
            title: 'ACCESSIBILITY.TITLE',
            subtitle: 'ACCESSIBILITY.SUBTITLE',
            avatar: { kind: 'icon', icon: 'accessibility' },
            width: 700,
        });
    }

    async openFileChooserDialog(
        data: Partial<FileChooserDialogData>,
    ): Promise<CardDialogRef<FileChooserDialogData, FileChooserDialogResult>> {
        const { FileChooserDialogComponent } = await import(
            './dialog-modules/file-chooser-dialog/file-chooser-dialog.module'
        );
        return this.cardDialog.open(FileChooserDialogComponent, {
            contentPadding: 0,
            height: 800,
            data: { ...new FileChooserDialogData(), ...(data ?? {}) },
        });
    }

    async openAddFolderDialog(
        data: AddFolderDialogData,
    ): Promise<CardDialogRef<AddFolderDialogData, AddFolderDialogResult>> {
        const { AddFolderDialogComponent } = await import(
            './dialog-modules/add-folder-dialog/add-folder-dialog.module'
        );
        return this.cardDialog.open(AddFolderDialogComponent, {
            title: 'WORKSPACE.ADD_FOLDER_TITLE',
            ...configForNode(data.parent),
            avatar: { kind: 'image', url: this.restConnector.getThemeMimeIconSvg('folder.svg') },
            width: 600,
            data,
        });
    }

    async openXmlAppPropertiesDialog(
        data: XmlAppPropertiesDialogData,
    ): Promise<CardDialogRef<XmlAppPropertiesDialogData, XmlAppPropertiesDialogResult>> {
        const title = await this.translate
            .get('ADMIN.APPLICATIONS.EDIT_APP', { xml: data.appXml })
            .toPromise();
        const { XmlAppPropertiesDialogComponent } = await import(
            './dialog-modules/xml-app-properties-dialog/xml-app-properties-dialog.module'
        );
        return this.cardDialog.open(XmlAppPropertiesDialogComponent, {
            title,
            width: 600,
            data,
        });
    }

    async openContributorEditDialog(
        data: ContributorEditDialogData,
    ): Promise<CardDialogRef<ContributorEditDialogData, ContributorEditDialogResult>> {
        const title = await this.translate
            .get('WORKSPACE.CONTRIBUTOR.' + (data.vCard ? 'EDIT' : 'ADD') + '_TITLE')
            .toPromise();
        const { ContributorEditDialogComponent } = await import(
            './dialog-modules/contributor-edit-dialog/contributor-edit-dialog.module'
        );
        return this.cardDialog.open(ContributorEditDialogComponent, {
            title,
            contentPadding: 0,
            width: 600,
            height: 900,
            closable: Closable.Standard,
            data,
        });
    }

    async openContributorsDialog(
        data: ContributorsDialogData,
    ): Promise<CardDialogRef<ContributorsDialogData, ContributorsDialogResult>> {
        const { ContributorsDialogComponent } = await import(
            './dialog-modules/contributors-dialog/contributors-dialog.module'
        );
        return this.cardDialog.open(ContributorsDialogComponent, {
            title: 'WORKSPACE.CONTRIBUTOR.TITLE',
            width: 500,
            height: 700,
            data,
        });
    }

    async openLicenseDialog(
        data: LicenseDialogData,
    ): Promise<CardDialogRef<LicenseDialogData, LicenseDialogResult>> {
        const { LicenseDialogComponent } = await import(
            './dialog-modules/license-dialog/license-dialog.module'
        );
        return this.cardDialog.open(LicenseDialogComponent, {
            title: 'WORKSPACE.LICENSE.TITLE',
            ...(data.kind === 'nodes'
                ? await configForNodes(data.nodes, this.translate).toPromise()
                : {}),
            width: 700,
            height: 1100,
            closable: Closable.Standard,
            data,
        });
    }

    async openShareDialog(
        data: ShareDialogData,
    ): Promise<CardDialogRef<ShareDialogData, ShareDialogResult>> {
        const { ShareDialogComponent } = await import(
            './dialog-modules/share-dialog/share-dialog.module'
        );
        return this.cardDialog.open(ShareDialogComponent, {
            title: 'WORKSPACE.SHARE.TITLE',
            contentPadding: 0,
            width: 900,
            height: 800,
            closable: Closable.Standard,
            data: { ...new ShareDialogData(), ...data },
        });
    }

    async openShareHistoryDialog(
        data: ShareHistoryDialogData,
    ): Promise<CardDialogRef<ShareHistoryDialogData, ShareHistoryDialogResult>> {
        const { ShareHistoryDialogComponent } = await import(
            './dialog-modules/share-history-dialog/share-history-dialog.module'
        );
        return this.cardDialog.open(ShareHistoryDialogComponent, {
            title: 'WORKSPACE.SHARE.HISTORY.TITLE',
            ...configForNode(data.node),
            contentPadding: 0,
            minWidth: 500,
            minHeight: 300,
            data,
        });
    }

    async openShareLinkDialog(
        data: ShareLinkDialogData,
    ): Promise<CardDialogRef<ShareLinkDialogData, ShareLinkDialogResult>> {
        const { ShareLinkDialogComponent } = await import(
            './dialog-modules/share-link-dialog/share-link-dialog.module'
        );
        return this.cardDialog.open(ShareLinkDialogComponent, {
            title: 'WORKSPACE.SHARE_LINK.TITLE',
            ...configForNode(data.node),
            width: 500,
            height: 700,
            data,
        });
    }

    async openCreateMapLinkDialog(
        data: CreateMapLinkDialogData,
    ): Promise<CardDialogRef<CreateMapLinkDialogData, CreateMapLinkDialogResult>> {
        const { CreateMapLinkDialogComponent } = await import(
            './dialog-modules/create-map-link-dialog/create-map-link-dialog.module'
        );
        return this.cardDialog.open(CreateMapLinkDialogComponent, {
            title: 'MAP_LINK.TITLE',
            ...configForNode(data.node),
            closable: Closable.Standard,
            width: 600,
            data,
        });
    }
}
