import { ComponentFixture, TestBed } from '@angular/core/testing';

import { H5pComponent } from './h5p.component';

describe('H5pComponent', () => {
    let component: H5pComponent;
    let fixture: ComponentFixture<H5pComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [H5pComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(H5pComponent);
        component = fixture.componentInstance;
        // Provide data so ngOnInit produces a real SafeResourceUrl for the iframe src;
        // without it the component's placeholder value triggers NG0904 (unsafe resource URL).
        component.data = {
            module: 'H5P',
            items: [
                {
                    link: 'https://example.com/h5p',
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
