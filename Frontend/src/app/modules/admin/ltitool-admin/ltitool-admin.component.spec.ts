import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LtitoolAdminComponent } from './ltitool-admin.component';

describe('LtitoolAdminComponent', () => {
    let component: LtitoolAdminComponent;
    let fixture: ComponentFixture<LtitoolAdminComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [LtitoolAdminComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(LtitoolAdminComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
