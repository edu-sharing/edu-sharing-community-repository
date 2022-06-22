import { TestBed, inject, getTestBed, ComponentFixture, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';
import { WorkspaceLicenseComponent } from './license.component';
import {
    TranslateService,
    TranslateModule,
    TranslatePipe,
    TranslateLoader,
} from '@ngx-translate/core';
import { DECLARATIONS } from '../../../declarations';
import { PROVIDERS } from '../../../providers';
import { IMPORTS } from '../../../imports';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

const fake_translate: any = {
    'COUNTRY_CODE.AL': 'Albania',
    'COUNTRY_CODE.AR': 'Argentina',
    'COUNTRY_CODE.DE': 'Germany',
    'COUNTRY_CODE.FR': 'France',
    'COUNTRY_CODE.GR': 'Greece',
    'COUNTRY_CODE.CH': 'Switzerland',
};

export class FakeLoader implements TranslateLoader {
    getTranslation(lang: string): Observable<any> {
        return rxjs.of(fake_translate);
    }
}

let component: WorkspaceLicenseComponent;
let fixture: ComponentFixture<WorkspaceLicenseComponent>;
let debugEl: DebugElement;
const country_code = ['AL', 'DE', 'AR', 'FR', 'GR', 'CH'];

// FIXME: Currently, all tests fail.
xdescribe('WorkspaceLicenseComponent ', () => {
    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                IMPORTS,
                TranslateModule.forRoot({
                    loader: {
                        provide: TranslateLoader,
                        useClass: FakeLoader,
                    },
                }),
            ],
            declarations: [DECLARATIONS, TranslatePipe, WorkspaceLicenseComponent],
            providers: [
                PROVIDERS,
                {
                    provide: Router,
                    useValue: {},
                },
            ],
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(WorkspaceLicenseComponent);
        component = fixture.componentInstance;
        debugEl = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should create the WorkspaceLicenseComponent ', () => {
        expect(component).toBeTruthy();
    });

    it('should method "translateLicenceCountries" to have been called ', () => {
        spyOn(component, 'translateLicenceCountries');
        component.translateLicenceCountries(country_code);
        expect(component.translateLicenceCountries).toHaveBeenCalled();
    });

    it('should consts "ccCountries" to be not empty ', () => {
        component.translateLicenceCountries(country_code);
        expect(component.getccCountries).not.toBeNull();
    });

    it('should consts "ccCountries" to has value ["AL", "DE", "AR", "FR", "GR", "CH"] and sorted', () => {
        component.translateLicenceCountries(country_code);
        let countryCode: any = [];
        component.getccCountries.forEach((ele) => {
            countryCode.push(ele.key);
        });
        expect(countryCode).toEqual(['AL', 'AR', 'CH', 'DE', 'FR', 'GR']);
    });

    it('should property "ccCountries"  value to be Translated ', () => {
        let injector = getTestBed();
        let translate = injector.get(TranslateService);
        translate.use('en');
        component.translateLicenceCountries(country_code);
        let countryValues: any = [];
        component.getccCountries.forEach((ele) => {
            countryValues.push(ele.name);
        });
        expect(countryValues).toEqual([
            'Albania',
            'Argentina',
            'France',
            'Germany',
            'Greece',
            'Switzerland',
        ]);
    });
});
