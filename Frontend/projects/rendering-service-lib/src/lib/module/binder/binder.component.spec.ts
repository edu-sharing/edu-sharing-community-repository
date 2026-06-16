import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BinderComponent } from './binder.component';

describe('BinderComponent', () => {
    let component: BinderComponent;
    let fixture: ComponentFixture<BinderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [BinderComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(BinderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
