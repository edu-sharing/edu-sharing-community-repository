import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { VideoComponent } from './video.component';
import { CurrentRenderRootUrlService } from '../../current-render-root-url.service';
import { AssetStateItem } from '../../dto/RenderData';

describe('VideoComponent', () => {
    let component: VideoComponent;
    let fixture: ComponentFixture<VideoComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [VideoComponent, TranslateModule.forRoot()],
            providers: [
                {
                    provide: CurrentRenderRootUrlService,
                    useValue: { rootUrl: 'http://localhost' },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(VideoComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('resolution menu', () => {
        const low: AssetStateItem = {
            link: 'http://localhost/public/240.mp4',
            progress: 100,
            height: 240,
            width: 426,
            status: 'FINISHED',
        };
        const high: AssetStateItem = {
            link: 'http://localhost/public/720.mp4',
            progress: 100,
            height: 720,
            width: 1280,
            status: 'FINISHED',
        };

        async function openMenu(): Promise<HTMLElement[]> {
            const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            trigger.click();
            fixture.detectChanges();
            await fixture.whenStable();
            return Array.from(document.querySelectorAll<HTMLElement>('.mat-mdc-menu-item'));
        }

        beforeEach(() => {
            component.data = { module: 'VIDEO', items: [low, high] };
            component.activeObject.set(high);
            fixture.detectChanges();
        });

        it('names the menu trigger', () => {
            const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('button');
            expect(trigger.getAttribute('aria-label')).toBe('RENDERING.VIDEO_RESOLUTION');
        });

        it('exposes the resolutions as radio menu items with the active one checked', async () => {
            const items = await openMenu();
            expect(items.length).toBe(2);
            expect(items.map((i) => i.getAttribute('role'))).toEqual([
                'menuitemradio',
                'menuitemradio',
            ]);
            expect(items.map((i) => i.getAttribute('aria-checked'))).toEqual(['false', 'true']);
        });
    });
});
