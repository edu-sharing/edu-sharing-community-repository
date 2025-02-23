import { AfterViewInit, Component, OnInit } from '@angular/core';
import { AboutService } from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { SpinnerComponent } from 'ngx-edu-sharing-ui';
import { RenderLegacyPageComponent } from '../render-legacy-page/render-legacy-page.component';
import { Render2PageComponent } from '../render2-page/render2-page.component';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Params, Router } from '@angular/router';

/**
 * wrapper that only decides which renderer (render2 or legacy) to lazy-load based on backend config
 */
@Component({
    selector: 'es-render-main-page',
    templateUrl: 'render-main-page.component.html',
    styleUrls: ['render-main-page.component.scss'],
    standalone: true,
    imports: [CommonModule, RenderLegacyPageComponent, Render2PageComponent, SpinnerComponent],
})
export class RenderMainPageComponent implements AfterViewInit {
    renderer: 'legacy' | 'render2' = null;
    private queryParams: Params;
    constructor(private about: AboutService, private route: ActivatedRoute) {
        this.route.queryParams.subscribe((params) => (this.queryParams = params));
        return;
    }
    async ngAfterViewInit() {
        try {
            if (this.queryParams.renderer) {
                this.renderer = this.queryParams.renderer;
                return;
            }
            this.renderer = (await firstValueFrom(this.about.getAbout())).plugins?.find(
                (f) => f.id === 'rendering-service-2',
            )
                ? 'render2'
                : 'legacy';
        } catch (e) {
            console.warn(e);
            this.renderer = 'legacy';
        }
    }
}
