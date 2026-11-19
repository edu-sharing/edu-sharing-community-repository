import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { ImageComponent } from './image.component';
import { CurrentRenderRootUrlService } from '../../current-render-root-url.service';
import { AssetStateItem } from '../../dto/RenderData';

describe('ImageComponent', () => {
    let component: ImageComponent;
    let fixture: ComponentFixture<ImageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ImageComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: CurrentRenderRootUrlService,
                    useValue: { rootUrl: 'http://localhost' },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ImageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    afterEach(() => {
        // Removes the instance-level override so the native Document getter applies again.
        delete (document as any).fullscreenElement;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('fullscreen toggle', () => {
        const item: AssetStateItem = {
            link: 'http://localhost/public/image.jpg',
            progress: 100,
            height: 600,
            width: 800,
            status: 'FINISHED',
        };

        function setFullscreenElement(element: Element | null) {
            Object.defineProperty(document, 'fullscreenElement', {
                configurable: true,
                get: () => element,
            });
            document.dispatchEvent(new Event('fullscreenchange'));
            fixture.detectChanges();
        }

        function button(): HTMLButtonElement {
            return fixture.nativeElement.querySelector('button');
        }

        beforeEach(() => {
            component.data = { module: 'IMAGE', items: [item] };
            component.activeObject.set(item);
            fixture.detectChanges();
        });

        it('offers to enter fullscreen while not in fullscreen', () => {
            expect(button().getAttribute('aria-label')).toBe('RENDERING.FULLSCREEN');
            expect(button().textContent?.trim()).toBe('fullscreen');
        });

        it('offers to exit fullscreen while its host is in fullscreen', () => {
            setFullscreenElement(fixture.nativeElement);
            expect(component.isFullscreen()).toBeTrue();
            expect(button().getAttribute('aria-label')).toBe('RENDERING.EXIT_FULLSCREEN');
            expect(button().textContent?.trim()).toBe('fullscreen_exit');
        });

        it('returns to the enter state after fullscreen is left', () => {
            setFullscreenElement(fixture.nativeElement);
            setFullscreenElement(null);
            expect(component.isFullscreen()).toBeFalse();
            expect(button().getAttribute('aria-label')).toBe('RENDERING.FULLSCREEN');
        });

        it('ignores fullscreen of another element', () => {
            setFullscreenElement(document.body);
            expect(component.isFullscreen()).toBeFalse();
            expect(button().getAttribute('aria-label')).toBe('RENDERING.FULLSCREEN');
        });
    });
});
