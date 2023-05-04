import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SortSelectPanelComponent } from './sort-select-panel.component';

describe('SortSelectPanelComponent', () => {
    let component: SortSelectPanelComponent;
    let fixture: ComponentFixture<SortSelectPanelComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [SortSelectPanelComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(SortSelectPanelComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
