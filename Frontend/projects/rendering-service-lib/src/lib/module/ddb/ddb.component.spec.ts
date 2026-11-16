import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { DdbComponent } from './ddb.component';

describe('DdbComponent', () => {
    let component: DdbComponent;
    let fixture: ComponentFixture<DdbComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DdbComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(DdbComponent);
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
            component.data = {
                module: 'DDB',
                items: [
                    {
                        link: '',
                        progress: 100,
                        height: 0,
                        width: 0,
                        status: 'FINISHED',
                        additionalData: {
                            size_small: '400,300',
                            widthPlaceHolder: '{width}',
                            heightPlaceHolder: '{height}',
                            linkTemplate: 'http://localhost/image/{width}x{height}',
                        },
                    },
                ],
            };
            component.activeObject.set('http://localhost/image/400x300');
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
    });
});
