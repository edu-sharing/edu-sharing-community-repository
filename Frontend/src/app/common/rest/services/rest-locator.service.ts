import {RestConstants} from "../rest-constants";
import {Observer} from "rxjs/Observer";
import {Headers, Http, RequestOptionsArgs, Response} from "@angular/http";
import {Observable} from "rxjs/Observable";
import {Translation} from "../../translation";
import {ActivatedRoute} from "@angular/router";
import {Injectable} from "@angular/core";
import {subscribeOn} from "rxjs/operator/subscribeOn";
import {CordovaService} from '../../services/cordova.service';

@Injectable()
export class RestLocatorService {
  private static ENDPOINT_URLS = [
    "rest/",
    "http://localhost:8080/edu-sharing/rest/",
    "http://localhost:8081/edu-sharing/rest/",
    "http://edu41.edu-sharing.de/edu-sharing/rest/",
    //"https://repository.oer-berlin.de/edu-sharing/rest/",
    "http://alfresco5.vm:8080/edu-sharing/rest/"
  ];
  private static DEFAULT_NUMBER_PER_REQUEST = 25;
  public numberPerRequest = RestLocatorService.DEFAULT_NUMBER_PER_REQUEST;


  private _endpointUrl : string;
  private _apiVersion=-1;
  private ticket: string;
  private themesUrl: any;
  private isLocating = false;

  get endpointUrl(): string {
    if(this.cordova.isRunningCordova())
      return this.cordova.endpointUrl;
    return this._endpointUrl;
  }
  get apiVersion(): number {
    return this._apiVersion;
  }

  set endpointUrl(value: string) {
    this._endpointUrl = value;
  }

  constructor(private http : Http,private cordova:CordovaService) {
  }
  public getConfig() : Observable<any>{
    return new Observable<any>((observer : Observer<any>) => {
      this.locateApi().subscribe(data => {
        let query = RestLocatorService.createUrl("config/:version/values", null);
        this.http.get(this.endpointUrl + query, this.getRequestOptions())
          .map((response: Response) => response.json())
          .subscribe(response => {
             this.setConfigValues(response.current);
             observer.next(response);
             observer.complete();
        },(error:any)=>{
            observer.error(error);
            observer.complete();
          });
      });
    });
  }
  public getConfigVariables() : Observable<string[]>{
    return new Observable<string[]>((observer : Observer<string[]>) => {
      this.locateApi().subscribe(data => {
        let query = RestLocatorService.createUrl("config/:version/variables", null);
        this.http.get(this.endpointUrl + query, this.getRequestOptions("application/json"))
          .map((response: Response) => response.json())
          .subscribe(response => {
            observer.next(response.current);
            observer.complete();
          },(error:any)=>{
            observer.error(error);
            observer.complete();
          });
      });
    });
  }
  public getConfigLanguage(lang:string) : Observable<any>{
    return new Observable<any>((observer : Observer<any>) => {
      this.locateApi().subscribe(data => {
        let query = RestLocatorService.createUrl("config/:version/language", null);
        this.http.get(this.endpointUrl + query, this.getRequestOptions("application/json",null,null,lang))
          .map((response: Response) => response.json())
          .subscribe(response => {
            observer.next(response.current);
            observer.complete();
          },(error:any)=>{
            observer.error(error);
            observer.complete();
          });
      });
    });
  }
  public getRequestOptions(contentType="application/json",username:string = null,password:string = null,locale=Translation.getISOLanguage()) : RequestOptionsArgs{
    let headers = new Headers();
    if(contentType)
      headers.append('Content-Type', contentType);
    headers.append('Accept', 'application/json');
    headers.append('locale',locale);
    if(username!=null) {
      headers.append('Authorization', "Basic " + btoa(username + ":" + password));
    }
    else if(this.ticket!=null){
      headers.append('Authorization', "EDU-TICKET " + this.ticket);
      this.ticket=null;
    }
    else if(this.cordova.oauth!=null){
      headers.append('Authorization', "Bearer " + this.cordova.oauth.access_token);
    }
    else{
      headers.append('Authorization',"");
    }

    return {headers:headers,withCredentials:true}; // Warn: withCredentials true will ignore a Bearer from OAuth!
  }
    private testApiCordova(observer : Observer<void>) : void {
      setTimeout(()=>{
        if(this.cordova.endpointUrl==null){
          this.testApiCordova(observer);
          return;
        }
        this.isLocating=false;
        observer.next(null);
        observer.complete();
      },100);
    }
    private testApi(pos : number,observer : Observer<void>) : void{
    if(pos==RestLocatorService.ENDPOINT_URLS.length)
      return;

    this.http.get(RestLocatorService.ENDPOINT_URLS[pos] + "_about", this.getRequestOptions(""))
      .map((response:Response)=>response.json())
      .subscribe((data:any)=> {
          this._endpointUrl=RestLocatorService.ENDPOINT_URLS[pos];
          this._apiVersion=data.version.major+data.version.minor/10;
          this.isLocating=false;
          this.themesUrl=data.themesUrl;
          console.log("API version "+this.apiVersion+" "+this._endpointUrl);
          observer.next(null);
          observer.complete();
          return;
        },
        (error:any)=>{
          if(error.status==RestConstants.HTTP_UNAUTHORIZED){
            this._endpointUrl=RestLocatorService.ENDPOINT_URLS[pos];
            this.isLocating=false;
            observer.next(null);
            observer.complete();
            return;
          }
          this.testApi(pos+1,observer);
        });
  }
  public locateApi() : Observable<void> {
    if(this.isLocating){
      return new Observable<void>((observer: Observer<void>) => {
        setTimeout(()=>{
          this.locateApi().subscribe(()=>{
            observer.next(null);
            observer.complete();
          });
        },20);
      });
    }
    if (this.endpointUrl != null) {
      return new Observable<void>((observer: Observer<void>) => {
        observer.next(null);
        observer.complete()
      });
    }
    this.isLocating=true;
    if(this.cordova.isRunningCordova()){
      console.log("locate via cordova");
      return new Observable<void>((observer: Observer<void>) => {
          this.testApiCordova(observer);
      });
    }
    return new Observable<void>((observer: Observer<void>) => {
      this.testApi(0,observer);
    });
  }

  setRoute(route: ActivatedRoute) : Observable<void> {
    return new Observable<void>((observer: Observer<void>) => {
      route.queryParams.subscribe((params: any) => {
        this.ticket = null;
        if (params["ticket"])
          this.ticket = params["ticket"];
        observer.next(null);
        observer.complete();
      });
    });
  }


  /**
   * Replaces jokers inside the url and espaces them. The default joker :version and :repository is always replaced!
   * @param url
   * @param repository the repo name
   * @param urlParams An array of params First value is the search joker, second the replace value
   * The search value may ends with |noescape. E.g. :sort|noescape. This tells the method to not escape the value content
   * @returns {string}
   */
  public static createUrl(url : string,repository : string,urlParams : string[][] = []) {
    for(let params of urlParams){
      params[1]=encodeURIComponent(params[1]);
    }
    return RestLocatorService.createUrlNoEscape(url,repository,urlParams);
  }


  /**
   * Same as createUrl, but does not escape the params. Escaping needs to be done when calling the method
   * @param url
   * @param repository
   * @param urlParams
   * @returns {string}
   */
  public static createUrlNoEscape(url : string,repository : string,urlParams : string[][] = []) {
    urlParams.push([":version",RestConstants.API_VERSION])
    urlParams.push([":repository",encodeURIComponent(repository)]);

    urlParams.sort(function(a,b){
      return url.indexOf(a[0])>url.indexOf(b[0]) ? 1 : -1;
    });
    let urlIn=url;
    let offset=0;
    for (let param of urlParams) {
      let pos=urlIn.indexOf(param[0]);
      if(pos==-1)
        continue;
      let start=url.substr(0,pos+offset);
      let end=url.substr(pos+offset+param[0].length,url.length);
      url=start+param[1]+end;
      offset+=param[1].length-param[0].length;
    }
    if(url.length>1000)
      console.warn("URL is "+url.length+" long");
    return url;
  }

  private setConfigValues(config: any) {
    if(config['itemsPerRequest'])
      this.numberPerRequest=config['itemsPerRequest'];
  }
}
