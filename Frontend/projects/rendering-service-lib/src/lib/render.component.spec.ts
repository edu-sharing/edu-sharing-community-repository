import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Toast } from 'ngx-edu-sharing-ui';
import { RenderComponent } from './render.component';

describe('RenderComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RenderComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Toast, useValue: { toast: (): void => {}, error: (): void => {} } },
            ],
        }).compileComponents();
    });

    it('should create the app', () => {
        const fixture = TestBed.createComponent(RenderComponent);
        const app = fixture.componentInstance;
        expect(app).toBeTruthy();
    });

    it('should render without a node', () => {
        const fixture = TestBed.createComponent(RenderComponent);
        expect(() => fixture.detectChanges()).not.toThrow();
    });
});
