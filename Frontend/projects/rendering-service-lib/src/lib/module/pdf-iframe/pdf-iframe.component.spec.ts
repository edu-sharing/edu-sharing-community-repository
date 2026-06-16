import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PdfIframeComponent } from './pdf-iframe.component';

describe('PdfComponent', () => {
    let component: PdfIframeComponent;
    let fixture: ComponentFixture<PdfIframeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PdfIframeComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(PdfIframeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
