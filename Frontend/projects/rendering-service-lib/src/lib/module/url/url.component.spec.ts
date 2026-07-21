import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Node } from 'ngx-edu-sharing-api';

import { UrlComponent } from './url.component';

describe('UrlComponent', () => {
    let component: UrlComponent;
    let fixture: ComponentFixture<UrlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UrlComponent, TranslateModule.forRoot()],
        }).compileComponents();

        fixture = TestBed.createComponent(UrlComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('builds the serlo embedding url from the external id', () => {
        const sanitizer = TestBed.inject(DomSanitizer);
        const spy = spyOn(sanitizer, 'bypassSecurityTrustResourceUrl').and.callThrough();
        component.externalId = '12345';

        component.getSerloUrl();

        expect(spy).toHaveBeenCalledWith('https://de.serlo.org/12345?contentOnly&hideBreadcrumbs');
    });

    it('builds the lti url from the virtual:ltiurl property with iframe launch params', () => {
        const sanitizer = TestBed.inject(DomSanitizer);
        const spy = spyOn(sanitizer, 'bypassSecurityTrustResourceUrl').and.callThrough();
        component.node = {
            ref: { id: 'abc' },
            properties: {
                'virtual:ltiurl': [
                    'https://repo/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=abc',
                ],
            },
        } as unknown as Node;

        component.getLtiUrl();

        expect(spy).toHaveBeenCalledWith(
            'https://repo/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=abc&editMode=false&launchPresentation=iframe',
        );
    });

    it('returns null for lti when virtual:ltiurl is missing', () => {
        component.node = { ref: { id: 'abc' }, properties: {} } as unknown as Node;

        expect(component.getLtiUrl()).toBeNull();
    });
});
