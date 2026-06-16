import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Toast } from 'ngx-edu-sharing-ui';

import { PdfIframeComponent } from './pdf-iframe.component';

describe('PdfComponent', () => {
    let component: PdfIframeComponent;
    let fixture: ComponentFixture<PdfIframeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PdfIframeComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Toast, useValue: { toast: (): void => {}, error: (): void => {} } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(PdfIframeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
