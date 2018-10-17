import { Component } from '@angular/core';
import {TranslateService, TranslateLoader} from "@ngx-translate/core";
import { Observable } from 'rxjs/Observable';
import 'rxjs/Rx';
import 'rxjs/add/observable/forkJoin';
import 'rxjs/add/observable/concat';
import {Observer} from "rxjs";
import {ConfigurationService} from "./services/configuration.service";
import {DatepickerOptions} from "ng2-datepicker";
import {ActivatedRoute} from "@angular/router";
import {SessionStorageService} from "./services/session-storage.service";
import 'rxjs/add/operator/first'
import {RestLocatorService} from "./rest/services/rest-locator.service";
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {CordovaService} from './services/cordova.service';
import * as moment from 'moment';

export var TRANSLATION_LIST=['common','admin','recycle','workspace', 'search','collections','login','permissions','oer','messages','services','override'];

export class Translation  {
  private static language : string;
  private static languageLoaded = false;
  /**
   * Initializes ng translate and returns the choosen language
   * @param translate
   */
  public static LANGUAGES:any={
    "de":"de_DE",
    "en":"en_US",
  };
  // none means that only labels should be shown (for dev)
  private static DEFAULT_SUPPORTED_LANGUAGES = ["de","en","none"];
  public static initialize(translate : TranslateService,config : ConfigurationService,storage:SessionStorageService,route:ActivatedRoute) : Observable<string> {
    return new Observable<string>((observer: Observer<string>) => {
      config.get("supportedLanguages",Translation.DEFAULT_SUPPORTED_LANGUAGES).subscribe((data: string[]) => {
        if(config.getLocator().getCordova().isRunningCordova()){
          Translation.initializeCordova(translate,config.getLocator().getCordova(),data).subscribe((language:string)=>{
            observer.next(language);
            observer.complete();
          });
          return;
        }
        translate.addLangs(data);
        //translate.setDefaultLang(data[0]);
        //translate.use(data[0]);
        //Translation.setLanguage(data[0]);
        storage.get("language").subscribe((storageLanguage)=> {
          route.queryParams.first().subscribe((params: any) => {
            let browserLang = translate.getBrowserLang();
            let language = data[0];
            let useStored=false;
            if (data.indexOf(params.locale) != -1) {
              language = params.locale;
            }
            else if (data.indexOf(storageLanguage) != -1) {
              language = storageLanguage;
              useStored=true;
            }
            else if (data.indexOf(browserLang) != -1) {
              language = browserLang;
            }
            if(!useStored)
              storage.set("language", language);
            if(language=="none"){
                translate.setDefaultLang(language);
            }
            else{
                translate.setDefaultLang(data[0]);
            }
            translate.use(language);
            Translation.setLanguage(language);
            translate.getTranslation(language).subscribe(()=>{
              Translation.languageLoaded=true;
              observer.next(language);
              observer.complete();
            });
          });
        });
      });
    });
  }
  public static initializeCordova(translate : TranslateService,cordova:CordovaService,supportedLanguages=Translation.DEFAULT_SUPPORTED_LANGUAGES) {
     return new Observable<string>((observer: Observer<string>) => {
          translate.addLangs(supportedLanguages);
          let language=supportedLanguages[0];
          translate.setDefaultLang(language);
          translate.use(language);
          Translation.setLanguage(language);
          cordova.getLanguage().subscribe((data: string) => {
              console.log("language from phone: "+data);
              if (supportedLanguages.indexOf(data) != -1) {
                  language=data;
              }
              translate.use(language);
              Translation.setLanguage(language);
              translate.getTranslation(language).subscribe(()=>{
                  Translation.languageLoaded=true;
                  observer.next(language);
                  observer.complete();
              });
          })
      });
  }
  public static isLanguageLoaded(){
    return Translation.languageLoaded;
  }
  static getLanguage() : string {
    return Translation.language;
  }
  static getISOLanguage() : string {
    return Translation.LANGUAGES[Translation.language];
  }
  private static setLanguage(language: string) {
    Translation.language = language;
  }
  static getDateFormat(){
    if(Translation.getLanguage()=="de"){
      return "DD.MM.YYYY";
    }
    return "YYYY/MM/DD";
  }
  static applyToDateOptions(translate:TranslateService,dateOptions: DatepickerOptions) {
    //dateOptions.locale=moment.localeData(this.getLanguage());
    dateOptions.displayFormat=Translation.getDateFormat();
    dateOptions.firstCalendarDay=1;
    dateOptions.locale=(moment.locale(this.getLanguage()) as any);
    /*
    dateOptions.todayText=translate.instant("TODAY");
    dateOptions.clearText=translate.instant("DATE_CLEAR");
    dateOptions.selectYearText=translate.instant("DATE_SELECT_YEAR");
    */
  }
}
export function createTranslateLoader(http: HttpClient,locator:RestLocatorService) {
  return new TranslationLoader(http,locator);
}
export class TranslationLoader implements TranslateLoader {
  constructor(private http: HttpClient,private locator : RestLocatorService, private prefix: string = "assets/i18n", private suffix: string = ".json") { }
  /**
   * Gets the translations from the server
   * @param lang
   * @returns {any}
   */

  public getTranslation(lang: string): Observable<any> {
    //return this.http.get(`${this.prefix}/common/${lang}${this.suffix}`)
    //  .map((res: Response) => res.json());
    if(lang=="none"){
        return new Observable<any>((observer : Observer<any>) => {
            observer.next({});
            observer.complete();
            console.log("initalized without translation");
        });
    }
    let translations : any =[];
    let results=0;
    let maxCount=TRANSLATION_LIST.length;
    for (let translation of TRANSLATION_LIST) {
      this.http.get(`${this.prefix}/${translation}/${lang}${this.suffix}`)
        .subscribe((data : any) => translations.push(data));

    }
    if(!this.locator.getCordova().isRunningCordova() || this.locator.getCordova().hasValidConfig()) {
      maxCount++;
      this.locator.getConfigLanguage(Translation.LANGUAGES[lang]).subscribe((data: any) => {
          translations.push(data);
      });
    }

    return new Observable<any>((observer : Observer<any>) => {
      let callback = ()=> {
        if (translations.length < maxCount) {
          setTimeout(callback, 10);
          return;
        }
        let final:any = {};
        for (const obj of translations) {
          for (const key in obj) {
            //copy all the fields
            let path=key.split(".");
            if(path.length==1) {
              final[key] = obj[key];
            }
          }
        }
        for (const obj of translations) {
          for (const key in obj) {
            let path=key.split(".");
            if(path.length==1) {
              continue;
            }
            else if(path.length==2){
              final[path[0]][path[1]]=obj[key];
            }
            else if(path.length==3){
              final[path[0]][path[1]][path[2]]=obj[key];
            }
            else if(path.length==4){
              final[path[0]][path[1]][path[2]][path[3]]=obj[key];
            }
          }
        }
        observer.next(final);
        observer.complete();
      };


      setTimeout(callback, 10);
    });
  }
}
