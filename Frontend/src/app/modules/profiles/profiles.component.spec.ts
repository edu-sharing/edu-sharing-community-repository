import { TestBed, async, inject, getTestBed, ComponentFixture } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TranslateService, TranslateModule, TranslatePipe, TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { ProfilesComponent } from './profiles.component';
import { DECLARATIONS } from '../../declarations';
import { IMPORTS } from '../../imports';
import { PROVIDERS } from '../../providers';


const fake_translate: any = {
  "SHOW_EMAIL_UPDATED": "Die E-Mail ist öffentlich",
  "HIDE_EMAIL_UPDATED": "Die E-Mail ist vertraulich",
  "USER":{
    "showEmail": "E-mail anzeigen",
    "hideEmail": "E-mail verbergen",
  }
}

export class FakeLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return Observable.of(fake_translate)
  }
}

let component: ProfilesComponent;
let fixture: ComponentFixture<ProfilesComponent>;
let debugEl: DebugElement;
describe('ProfilesComponent ', () => {
  beforeEach(() => {
    fixture = TestBed.createComponent(ProfilesComponent);
    component = fixture.componentInstance;
    debugEl = fixture.debugElement;
    fixture.detectChanges();
  })

  it('should create the ProfilesComponent ', () => {
    expect(component).toBeTruthy();
  });

  // it('should method "translateLicenceCountries" to have been called ', () => {
  //   spyOn(component, "translateLicenceCountries");
  //   component.translateLicenceCountries(country_code)
  //   expect(component.translateLicenceCountries).toHaveBeenCalled();
  // });


  // it('should consts "ccCountries" to be not empty ', () => {
  //   component.translateLicenceCountries(country_code)
  //   expect(component.getccCountries).not.toBeNull();
  // });

  // it('should consts "ccCountries" to has value ["AL", "DE", "AR", "FR", "GR", "CH"] and sorted', () => {
  //   component.translateLicenceCountries(country_code)
  //   let countryCode: any = [];
  //   component.getccCountries.forEach(ele => { countryCode.push(ele.key) });
  //   expect(countryCode).toEqual(['AL', 'AR', 'CH', 'DE', 'FR', 'GR']);
  // });

  // it('should property "ccCountries"  value to be Translated ', () => {
  //   let injector = getTestBed();
  //   let translate = injector.get(TranslateService);
  //   translate.use('en');
  //   component.translateLicenceCountries(country_code)
  //   let countryValues: any = [];
  //   component.getccCountries.forEach(ele => { countryValues.push(ele.name) });
  //   expect(countryValues).toEqual(['Albania', 'Argentina', 'France', 'Germany', 'Greece', 'Switzerland']);
  // });

})