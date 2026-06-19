import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { UrlComponent } from './url.component';

describe('UrlComponent', () => {
    let component: UrlComponent;
    let fixture: ComponentFixture<UrlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UrlComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(UrlComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
