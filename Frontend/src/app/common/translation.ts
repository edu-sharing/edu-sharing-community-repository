import { Component } from '@angular/core';
import {TranslateService, TranslateLoader} from "@ngx-translate/core";
import { Http, Response, Headers } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/Rx';
import 'rxjs/add/observable/forkJoin';
import 'rxjs/add/observable/concat';
import {Observer} from "rxjs";
import {ConfigurationService} from "./services/configuration.service";
import {DatepickerOptions} from "ng2-datepicker";
import * as moment from 'moment';
import {ActivatedRoute} from "@angular/router";
import {SessionStorageService} from "./services/session-storage.service";
import 'rxjs/add/operator/first'

export var TRANSLATION_LIST=['common','admin','recycle','workspace', 'search','collections','login','permissions','oer','messages','override'];

export class Translation  {
  private static language : string;
  private static languageLoaded = false;
  /**
   * Initializes ng translate and returns the choosen language
   * @param translate
   */
  private static LANGUAGES:any={
    "de":"de_DE",
    "en":"en_US",
  };
  public static initialize(translate : TranslateService,config : ConfigurationService,storage:SessionStorageService,route:ActivatedRoute) : Observable<string> {
    return new Observable<string>((observer: Observer<string>) => {
      config.get("supportedLanguages").subscribe((data: string[]) => {
        translate.addLangs(data);
        translate.setDefaultLang(data[0]);
        translate.use(data[0]);
        Translation.setLanguage(data[0]);
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
            translate.use(language);
            Translation.setLanguage(language);
            Translation.languageLoaded=true;
            observer.next(language);
            observer.complete();
          });
        });

      });
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
    console.log("language","use "+language);
    Translation.language = language;
  }
  static getDateFormat(){
    if(Translation.getLanguage()=="de"){
      return "DD.MM.YYYY";
    }
    return "YYYY/MM/DD";
  }
  static applyToDateOptions(translate:TranslateService,dateOptions: DatepickerOptions) {
    moment.locale(this.getLanguage());
    console.log(this.getLanguage());
    //dateOptions.locale=moment.localeData(this.getLanguage());
    dateOptions.displayFormat=Translation.getDateFormat();
    /*
    dateOptions.todayText=translate.instant("TODAY");
    dateOptions.clearText=translate.instant("DATE_CLEAR");
    dateOptions.selectYearText=translate.instant("DATE_SELECT_YEAR");
    */
  }
}
export function createTranslateLoader(http: Http) {
  return new TranslationLoader(http);
}
export class TranslationLoader implements TranslateLoader {
  constructor(private http: Http, private prefix: string = "assets/i18n", private suffix: string = ".json") { }
  /**
   * Gets the translations from the server
   * @param lang
   * @returns {any}
   */

  public getTranslation(lang: string): Observable<any> {
    //return this.http.get(`${this.prefix}/common/${lang}${this.suffix}`)
    //  .map((res: Response) => res.json());
    var translations : any =[];
    var results=0;
    for (let translation of TRANSLATION_LIST) {
      this.http.get(`${this.prefix}/${translation}/${lang}${this.suffix}`)
        .map((res: Response) => res.json()).subscribe((data : any) => translations.push(data));

    }
    return new Observable<any>((observer : Observer<any>) => {
      let callback = ()=> {
        if (translations.length < TRANSLATION_LIST.length) {
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
              continue
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
