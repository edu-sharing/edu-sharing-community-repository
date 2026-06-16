import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DdbComponent } from './ddb.component';

describe('DdbComponent', () => {
    let component: DdbComponent;
    let fixture: ComponentFixture<DdbComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DdbComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(DdbComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
