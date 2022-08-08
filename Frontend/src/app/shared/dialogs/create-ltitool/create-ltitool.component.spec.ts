import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateLtitoolComponent } from './create-ltitool.component';

describe('CreateLtitoolComponent', () => {
    let component: CreateLtitoolComponent;
    let fixture: ComponentFixture<CreateLtitoolComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CreateLtitoolComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateLtitoolComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
