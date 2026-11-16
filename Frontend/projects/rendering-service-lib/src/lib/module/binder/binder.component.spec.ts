import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { BinderComponent } from './binder.component';

describe('BinderComponent', () => {
    let component: BinderComponent;
    let fixture: ComponentFixture<BinderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [BinderComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(BinderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
