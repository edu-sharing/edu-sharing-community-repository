import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { BinderComponent } from './binder.component';

describe('BinderComponent', () => {
    let component: BinderComponent;
    let fixture: ComponentFixture<BinderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [BinderComponent],
            providers: [provideTranslateService()],
        }).compileComponents();

        fixture = TestBed.createComponent(BinderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
