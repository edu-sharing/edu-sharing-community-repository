import { Component, computed, inject, resource, signal } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { OptionItem } from 'ngx-edu-sharing-ui';
import { SharedModule } from '../../../../../shared/shared.module';
import {
    TopicPageEntries,
    TopicPageEntry,
    TopicPageManageService,
} from '../../../shared/services/topic-page-manage.service';
import { retrieveNodeId } from '../../../shared/utils/template-util';
import {
    createOptionLabel,
    TOPIC_PAGE_SIDEBAR_HOST,
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
 * show, regenerate, publish as a global template or delete them.
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
    readonly settingsPrefix: string = 'TOPIC_PAGE.SIDE_MENU.CONFIG_PAGE_VARIANT.';
    readonly templateUpdatePrefix: string = this.settingsPrefix + 'TEMPLATE_UPDATE.';
    readonly host: TopicPageSidebarHost = inject(TOPIC_PAGE_SIDEBAR_HOST);
    readonly busy = signal(false);
    // the actions of the entry whose menu is open
    readonly menuOptions = signal<OptionItem[]>([]);
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

    /**
     * The actions of the entry's menu besides showing and regenerating it.
     */
    entryOptions(entry: TopicPageEntry): OptionItem[] {
        const options: OptionItem[] = [];
        if (entry.templateEditTarget) {
            options.push(
                new OptionItem(this.i18nPrefix + 'EDIT_TEMPLATE', 'edit', () =>
                    this.editTemplate(entry),
                ),
            );
        }
        if (entry.kind === 'inherited') {
            return options;
        }
        if (this.host.canAddToGlobalTemplates()) {
            const addToGlobalTemplates = new OptionItem(
                this.settingsPrefix + 'ADD_TO_GLOBAL_TEMPLATES',
                'public',
                () => this.addToGlobalTemplates(entry),
            );
            // a global template is told apart from the others by its parameters
            addToGlobalTemplates.isEnabled = this.host.parametersComplete(entry.node);
            options.push(addToGlobalTemplates);
        }
        options.push(
            new OptionItem(this.i18nPrefix + 'DELETE', 'delete', () => this.delete(entry)),
        );
        return options;
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

    async addToGlobalTemplates(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.addEntryToGlobalTemplates(entry));
    }

    async delete(entry: TopicPageEntry): Promise<void> {
        await this.run(() => this.host.deleteEntry(entry));
    }

    create(): void {
        this.topicPageSidebarService.open('variant');
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
