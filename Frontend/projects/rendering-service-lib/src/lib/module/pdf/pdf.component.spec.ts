import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { Toast } from 'ngx-edu-sharing-ui';

import { PdfComponent } from './pdf.component';

describe('PdfComponent', () => {
    let component: PdfComponent;
    let fixture: ComponentFixture<PdfComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PdfComponent],
            providers: [
                provideTranslateService(),
                { provide: Toast, useValue: { toast: (): void => {}, error: (): void => {} } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(PdfComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
