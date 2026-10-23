import { Component, computed, inject, input, InputSignal, output, Signal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { TooltipAriaLabelDirective } from '../../shared/directives/tooltip-aria-label.directive';
import { TopicPageHistoryService } from '../../shared/services/topic-page-history.service';
import { TopicPageVariantPreviewService } from '../../shared/services/topic-page-variant-preview.service';
import { HistoryStep } from '../../shared/types/history-step';
import { CreationKind, VariantPreview } from '../../shared/types/variant-preview';

// the page states the bar stands for, which exclude each other
export type TopicPageStatusBarMode = 'preview' | 'globalTemplate' | 'template' | 'edit';

export interface SwimlaneCompletionCounts {
    unfinished: number;
    partial: number;
}

/**
 * The single bar above the topic page's breadcrumb: names the page's mode and holds its actions,
 * the edit history and the hint at swimlanes still to be finished.
 */
@Component({
    selector: 'es-topic-page-status-bar',
    imports: [
        EduSharingUiCommonModule,
        MatButton,
        MatIconButton,
        MatTooltip,
        TooltipAriaLabelDirective,
        TranslateModule,
    ],
    templateUrl: './topic-page-status-bar.component.html',
    styleUrls: ['./topic-page-status-bar.component.scss'],
})
export class TopicPageStatusBarComponent {
    protected history = inject(TopicPageHistoryService);
    private variantPreviewService = inject(TopicPageVariantPreviewService);

    readonly i18nPrefix: string = 'TOPIC_PAGE.';

    mode: InputSignal<TopicPageStatusBarMode> = input.required();
    editing: InputSignal<boolean> = input(false);
    busy: InputSignal<boolean> = input(false);
    globalTemplateNode: InputSignal<Node | null> = input(null);
    completion: InputSignal<SwimlaneCompletionCounts | null> = input(null);
    stepLabel: InputSignal<(step: HistoryStep) => string> = input.required();

    readonly jumpToIncomplete = output<void>();
    readonly leave = output<void>();
    readonly backToSelection = output<CreationKind>();

    protected preview: Signal<VariantPreview | null> = this.variantPreviewService.preview;
    protected previewNode: Signal<Node | null> = this.variantPreviewService.node;
    protected showHistory: Signal<boolean> = computed(
        (): boolean => this.editing() && this.mode() !== 'preview',
    );
    protected completionTotal: Signal<number> = computed((): number => {
        const counts: SwimlaneCompletionCounts | null = this.completion();
        return counts ? counts.unfinished + counts.partial : 0;
    });

    showPreviewAt(index: number): void {
        void this.variantPreviewService.showAt(index);
    }

    usePreview(): void {
        void this.variantPreviewService.use();
    }

    endPreview(): void {
        void this.variantPreviewService.end();
    }
}
