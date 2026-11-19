import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { NodeHelperService } from 'ngx-edu-sharing-ui';

import { DefaultComponent } from './default.component';

describe('DefaultComponent', () => {
    let component: DefaultComponent;
    let fixture: ComponentFixture<DefaultComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DefaultComponent],
            providers: [
                provideTranslateService(),
                {
                    provide: NodeHelperService,
                    useValue: jasmine.createSpyObj('NodeHelperService', ['getNodesRight']),
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(DefaultComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
