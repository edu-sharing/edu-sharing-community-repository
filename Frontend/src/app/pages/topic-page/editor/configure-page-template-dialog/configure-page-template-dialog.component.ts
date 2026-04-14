import { Component, EventEmitter, Input, Output, signal, WritableSignal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { v4 as uuidv4 } from 'uuid';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import { CardDialogRef } from '../../../../features/dialogs/card-dialog/card-dialog-ref';
import { SharedModule } from '../../../../shared/shared.module';
import { TopicPageHelperService } from '../../shared/services/topic-page-helper.service';
import {
    DEFAULT_PAGE_CONFIG_ASPECT,
    DEFAULT_PAGE_CONFIG_PROP,
    DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
    DEFAULT_PAGE_NAME_PREFIX,
    DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
    DEFAULT_PAGE_VARIANT_CONFIG_PROP,
    DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP,
    DEFAULT_PAGE_VARIANT_NAME_PREFIX,
} from '../../shared/types/custom-definitions';
import { PageConfig } from '../../shared/types/page-config';
import { PageVariantConfig } from '../../shared/types/page-variant-config';
import { prependWorkspacePrefix, retrieveNodeId } from '../../shared/utils/template-util';

@Component({
    selector: 'es-configure-page-template-dialog',
    imports: [SharedModule],
    templateUrl: 'configure-page-template-dialog.component.html',
    styleUrls: ['configure-page-template-dialog.component.scss'],
})
export class ConfigurePageTemplateDialogComponent {
    readonly i18nPrefix: string = 'TOPIC_PAGE.CONFIG_PAGE_TEMPLATE.';
    @Input() dialogRef: CardDialogRef;
    @Input() collectionId: string;
    @Input() pageConfigRef: string;
    @Input() pageConfigPropagateRef: string;
    @Output() switchIntoTemplateMode: EventEmitter<boolean> = new EventEmitter<boolean>();

    creationInProgress: WritableSignal<boolean> = signal(false);

    constructor(private topicPageHelperService: TopicPageHelperService) {}

    /**
     * Resets the page config propagate ref and reloads the page,
     * if the same page config ref and page config propagate ref exist.
     */
    async unlinkPageConfigPropagate() {
        await this.topicPageHelperService.resetProperty(
            this.collectionId,
            DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
        );
        window.location.reload();
    }

    /**
     * Creates and links an empty page config.
     */
    async createAndLinkEmptyPageConfig() {
        this.creationInProgress.set(true);
        try {
            // page ccm:map for page config node
            const pageConfigNode: Node = await this.topicPageHelperService.createChild(
                this.collectionId,
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_CONFIG_ASPECT,
            );
            // create page variant as template and add it to the list of variants
            const pageVariants: string[] = [];
            const properties: { [key: string]: string } = {
                [DEFAULT_PAGE_VARIANT_IS_TEMPLATE_PROP]: 'true',
            };
            let pageConfigVariantNode: Node = await this.topicPageHelperService.createChild(
                retrieveNodeId(pageConfigNode),
                RestConstants.CCM_TYPE_MAP,
                DEFAULT_PAGE_VARIANT_NAME_PREFIX + uuidv4(),
                DEFAULT_PAGE_VARIANT_CONFIG_ASPECT,
                properties,
            );
            pageVariants.push(prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)));
            // update properties of the page variant afterward to include the node ID in the template
            // @TODO
            const variantConfig: PageVariantConfig = {
                // variables: {
                //     [DEFAULT_MDS_WIDGET_PREFIX + 'profiling_widget_intention']: ['learn'],
                //     [DEFAULT_MDS_WIDGET_PREFIX + 'profiling_widget_education_level']: ['http://w3id.org/openeduhub/vocabs/educationalContext/elementarbereich']
                // },
                template: {
                    id: prependWorkspacePrefix(retrieveNodeId(pageConfigVariantNode)),
                    version: '1.0.0',
                },
                structure: {
                    swimlanes: [],
                },
            };
            await this.topicPageHelperService.setProperty(
                retrieveNodeId(pageConfigVariantNode),
                DEFAULT_PAGE_VARIANT_CONFIG_PROP,
                JSON.stringify(variantConfig),
            );
            // update page config node
            const pageConfig: PageConfig = {
                default: pageVariants[0],
                variants: pageVariants,
            };
            await this.topicPageHelperService.setProperty(
                retrieveNodeId(pageConfigNode),
                DEFAULT_PAGE_CONFIG_PROP,
                JSON.stringify(pageConfig),
            );
            // update propagate ref
            await this.topicPageHelperService.setProperty(
                this.collectionId,
                DEFAULT_PAGE_CONFIG_PROPAGATE_REF_PROP,
                prependWorkspacePrefix(retrieveNodeId(pageConfigNode)),
            );
            this.switchToPagePropagateRef(true);
            this.creationInProgress.set(false);
        } catch (e) {
            console.error(e);
            this.creationInProgress.set(false);
        }
    }

    /**
     * Switches to the page config propagate ref.
     *
     * @param reloadNecessary
     */
    switchToPagePropagateRef(reloadNecessary: boolean = false) {
        this.dialogRef.close();
        this.switchIntoTemplateMode.emit(reloadNecessary);
    }
}
