import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Oauth2consentPageComponent } from './oauth2consent-page.component';

describe('Oauth2consentPageComponent', () => {
    let component: Oauth2consentPageComponent;
    let fixture: ComponentFixture<Oauth2consentPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [Oauth2consentPageComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(Oauth2consentPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
