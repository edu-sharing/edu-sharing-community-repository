import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { Node } from 'ngx-edu-sharing-api';
import { UrlEmbeddings } from '../../dto/FrontendModuleConfig';
import { GdprConfig } from '../../dto/GdprConfig';

import { UrlComponent } from './url.component';

describe('UrlComponent', () => {
    let component: UrlComponent;
    let fixture: ComponentFixture<UrlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UrlComponent, TranslateModule.forRoot()],
            // The link and consent views use icon and config services that issue http requests.
            providers: [provideHttpClient(), provideHttpClientTesting()],
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

    it('uses the virtual:connectorrenderurl property unchanged for simple connectors', () => {
        const sanitizer = TestBed.inject(DomSanitizer);
        const spy = spyOn(sanitizer, 'bypassSecurityTrustResourceUrl').and.callThrough();
        component.node = {
            ref: { id: 'abc' },
            properties: {
                'virtual:connectorrenderurl': ['https://connector.tld/render?nodeId=abc'],
            },
        } as unknown as Node;

        component.getConnectorRenderUrl();

        expect(spy).toHaveBeenCalledWith('https://connector.tld/render?nodeId=abc');
    });

    it('returns null for a simple connector when virtual:connectorrenderurl is missing', () => {
        component.node = { ref: { id: 'abc' }, properties: {} } as unknown as Node;

        expect(component.getConnectorRenderUrl()).toBeNull();
    });

    it('moves focus to the link button after consenting to the gdpr notice', async () => {
        spyOn(window, 'open').and.returnValue(null);
        component.embedding = UrlEmbeddings.LINK;
        component.url = 'https://example.org';
        component.gdpr = {
            name: 'EXTERNAL_SOURCES',
            ref: 'https://example.org/privacy',
        } as GdprConfig;
        fixture.detectChanges();
        const consentButton = fixture.nativeElement.querySelector(
            '.external-embed-content button',
        ) as HTMLButtonElement;
        consentButton.focus();

        consentButton.click();
        fixture.detectChanges();
        await fixture.whenStable();

        const link = fixture.nativeElement.querySelector('.link-wrapper a');
        expect(link).toBeTruthy();
        expect(document.activeElement).toBe(link);
    });
});
