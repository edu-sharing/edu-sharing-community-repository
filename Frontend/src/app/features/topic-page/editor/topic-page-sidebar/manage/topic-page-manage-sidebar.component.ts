import { Component, computed, inject, resource, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { SharedModule } from '../../../../../shared/shared.module';
import {
    TopicPageEntries,
    TopicPageEntry,
    TopicPageManageService,
} from '../../../shared/services/topic-page-manage.service';
import { retrieveNodeId } from '../../../shared/utils/template-util';
import {
    createOptionLabel,
    TopicPageSidebarHost,
    TopicPageSidebarService,
} from '../topic-page-sidebar.service';

type EntrySection = {
    key: 'inherited' | 'variants' | 'templates';
    entries: TopicPageEntry[];
    heading: string;
    headingParams?: Record<string, string>;
    description?: string;
    // the collection's own pages right below the intro need no heading of their own
    hideHeading?: boolean;
};

/**
 * Entry of the topic page's sidebar menu that lists the pages and templates of the collection, to
 * show, regenerate or delete them.
 */
@Component({
    selector: 'es-topic-page-manage-sidebar',
    imports: [SharedModule],
    templateUrl: './topic-page-manage-sidebar.component.html',
    styleUrls: ['./topic-page-manage-sidebar.component.scss'],
})
export class TopicPageManageSidebarComponent {
    private readonly topicPageSidebarService = inject(TopicPageSidebarService);
    private readonly manageService = inject(TopicPageManageService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.SIDEBAR.MANAGE.';
    readonly templateUpdatePrefix: string =
        'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.TEMPLATE_UPDATE.';
    readonly host: TopicPageSidebarHost = this.topicPageSidebarService.host;
    readonly busy = signal(false);
    readonly createLabel = computed((): string => createOptionLabel(this.host.creationKinds()));
    private readonly revision = signal(0);
    // the shown page changes when it is regenerated, so the list is read again with it
    readonly entries = resource({
        params: () => ({
            collectionId: this.host.collectionNodeId(),
            includeTemplates: this.host.canEditTemplates(),
            pageVariantNode: this.host.pageVariantNode(),
            revision: this.revision(),
        }),
        loader: ({ params }): Promise<TopicPageEntries> =>
            this.manageService.load(params.collectionId, params.includeTemplates),
    });
    readonly sections = computed((): EntrySection[] => {
        const entries: TopicPageEntries | undefined = this.entries.hasValue()
            ? this.entries.value()
            : undefined;
        if (!entries) {
            return [];
        }
        const sections: EntrySection[] = [
            {
                key: 'inherited',
                entries: entries.inherited,
                heading: 'INHERITED_HEADING',
                headingParams: {
                    title: entries.inheritedFrom?.title || entries.inheritedFrom?.name,
                },
            },
            { key: 'variants', entries: entries.variants, heading: 'VARIANTS_HEADING' },
            {
                key: 'templates',
                entries: entries.templates,
                heading: 'TEMPLATES_HEADING',
                description: 'TEMPLATES_DESCRIPTION',
            },
        ];
        const shown: EntrySection[] = sections.filter(
            (section: EntrySection) => section.entries.length,
        );
        return shown.map((section: EntrySection, index: number) => ({
            ...section,
            hideHeading: index === 0 && section.key === 'variants',
        }));
    });

    isCurrent(entry: TopicPageEntry): boolean {
        return (
            (entry.kind === 'template') === this.host.templateMode() &&
            retrieveNodeId(entry.node) === retrieveNodeId(this.host.pageVariantNode())
        );
    }

    templateDate(entry: TopicPageEntry): string | undefined {
        return entry.templateNode?.modifiedAt;
    }

    createdBy(node: Node): string {
        const person = node.createdBy;
        return [person?.firstName, person?.lastName].filter((part) => !!part).join(' ');
    }

    async show(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.showEntry(entry));
    }

    async regenerate(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.regenerateEntry(entry));
    }

    async editTemplate(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.editTemplate(entry));
    }

    async delete(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.deleteEntry(entry));
    }

    create(): void {
        this.topicPageSidebarService.open('variant', false);
    }

    private async run(action: () => Promise<void>): Promise<void> {
        this.busy.set(true);
        try {
            await action();
        } finally {
            this.busy.set(false);
            this.revision.update((revision: number) => revision + 1);
        }
    }
}
