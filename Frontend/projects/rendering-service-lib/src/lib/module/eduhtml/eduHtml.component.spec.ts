import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EduHtmlComponent } from './eduHtml.component';

describe('EduHtmlComponent', () => {
    let component: EduHtmlComponent;
    let fixture: ComponentFixture<EduHtmlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EduHtmlComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(EduHtmlComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
