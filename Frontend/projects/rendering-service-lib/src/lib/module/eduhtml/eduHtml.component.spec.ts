import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { EduHtmlComponent } from './eduHtml.component';

describe('EduHtmlComponent', () => {
    let component: EduHtmlComponent;
    let fixture: ComponentFixture<EduHtmlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EduHtmlComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(EduHtmlComponent);
        component = fixture.componentInstance;
        // Provide data so ngOnInit produces a real SafeResourceUrl for the iframe src;
        // without it the component's placeholder value triggers NG0904 (unsafe resource URL).
        component.data = {
            module: 'EDUHTML',
            items: [
                {
                    link: 'https://example.com/eduhtml',
                    progress: 100,
                    height: 0,
                    width: 0,
                    status: 'FINISHED',
                },
            ],
        };
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
