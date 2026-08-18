import { AfterViewInit, Component, signal, inject } from '@angular/core';
import { RenderHelperService, SpinnerComponent } from 'ngx-edu-sharing-ui';
import { RenderLegacyPageComponent } from '../render-legacy-page/render-legacy-page.component';
import { Render2PageComponent } from '../render2-page/render2-page.component';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Params } from '@angular/router';

/**
 * wrapper that only decides which renderer (render2 or legacy) to lazy-load based on backend config
 */
@Component({
    selector: 'es-render-main-page',
    templateUrl: 'render-main-page.component.html',
    styleUrls: ['render-main-page.component.scss'],
    imports: [CommonModule, RenderLegacyPageComponent, Render2PageComponent, SpinnerComponent],
})
export class RenderMainPageComponent implements AfterViewInit {
    private renderHelper = inject(RenderHelperService);
    private route = inject(ActivatedRoute);

    renderer = signal<'legacy' | 'render2'>(null);
    private queryParams: Params;
    constructor() {
        this.route.queryParams.subscribe((params) => (this.queryParams = params));
        return;
    }
    async ngAfterViewInit() {
        try {
            if (this.queryParams.renderer) {
                this.renderer.set(this.queryParams.renderer);
                return;
            }
            this.renderer.set(
                (await this.renderHelper.hasRenderingService2()) ? 'render2' : 'legacy',
            );
        } catch (e) {
            console.warn(e);
            this.renderer.set('legacy');
        }
    }
}
