import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { SharedModule } from '../../../../../shared/shared.module';
import { retrieveNodeId } from '../../../shared/utils/template-util';
import { ConfigurePageVariantOrTemplateComponent } from '../../configure-page-variant-or-template/configure-page-variant-or-template.component';
import {
    TOPIC_PAGE_SIDEBAR_HOST,
    TopicPageSettings,
    TopicPageSidebarHost,
} from '../topic-page-sidebar.service';

/**
 * Entry of the topic page's sidebar menu that edits the title and parameters of the page or
 * template shown in the editor.
 */
@Component({
    selector: 'es-topic-page-settings-sidebar',
    imports: [SharedModule, ConfigurePageVariantOrTemplateComponent],
    templateUrl: './topic-page-settings-sidebar.component.html',
    styleUrls: ['./topic-page-settings-sidebar.component.scss'],
    // the settings are read from plain fields of the editor, not only from signals
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class TopicPageSettingsSidebarComponent {
    readonly host: TopicPageSidebarHost = inject(TOPIC_PAGE_SIDEBAR_HOST);

    // the form reads its values once, so another node starts it anew
    settingsKey(settings: TopicPageSettings): string {
        return [
            settings.globalTemplateMode,
            settings.templateMode,
            retrieveNodeId(settings.node),
        ].join(':');
    }
}
