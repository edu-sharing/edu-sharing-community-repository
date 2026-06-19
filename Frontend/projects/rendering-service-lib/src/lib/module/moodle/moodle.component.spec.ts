import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { MoodleComponent } from './moodle.component';

describe('MoodleComponent', () => {
    let component: MoodleComponent;
    let fixture: ComponentFixture<MoodleComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [MoodleComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(MoodleComponent);
        component = fixture.componentInstance;
        // Provide data so ngOnInit produces a real SafeResourceUrl for the iframe src;
        // without it the iframe binding triggers NG0904 (unsafe resource URL).
        component.data = {
            module: 'MOODLE',
            items: [
                {
                    link: 'https://example.com/course',
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
