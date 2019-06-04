import {RestConstants} from "../rest-constants";
import {Observer} from "rxjs";
import {Observable} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {Injectable} from "@angular/core";
import {subscribeOn} from "rxjs/operator/subscribeOn";
import {environment} from "../../../../environments/environment";
import {HttpClient, HttpHeaders, HttpResponse} from '@angular/common/http';
import {OAuthResult} from "../data-object";
import {BridgeService} from "../../../core-bridge-module/bridge.service";

@Injectable()
export class RestLocatorService {
  private static DEFAULT_NUMBER_PER_REQUEST = 25;
  public numberPerRequest = RestLocatorService.DEFAULT_NUMBER_PER_REQUEST;


  private _endpointUrl : string;
  private _apiVersion=-1;
  private ticket: string;
  private themesUrl: any;
  private isLocating = false;

  get endpointUrl(): string {
    return this._endpointUrl;
  }
  get apiVersion(): number {
    return this._apiVersion;
  }

  set endpointUrl(value: string) {
    this._endpointUrl = value;
  }

  constructor(private http : HttpClient,private bridge:BridgeService) {
  }
  public createOAuthFromSession(){
      return new Observable((observer : Observer<OAuthResult>) => {
          this.bridge.getCordova().loginOAuth(this.endpointUrl,null,null,"client_credentials").subscribe((oauthTokens) => {
              this.bridge.getCordova().setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS, JSON.stringify(oauthTokens));
              observer.next(oauthTokens);
              observer.complete();
          },(error)=>{
              observer.error(error);
              observer.complete();
          });
    });
  }
    public getConfigDynamic(key:string) : Observable<any>{
        return new Observable<any>((observer : Observer<any>) => {
            this.locateApi().subscribe(data => {
                let query = RestLocatorService.createUrl("config/:version/dynamic/:key", null,[[":key",key]]);
                this.http.get<any>(this.endpointUrl + query, this.getRequestOptions())
                    .subscribe(response => {
                        // unmarshall encapuslated json response
                        observer.next(JSON.parse(response.body.value));
                        observer.complete();
                    },(error:any)=>{
                        observer.error(error);
                        observer.complete();
                    });
            });
        });
    }
  public getConfig() : Observable<any>{
    return new Observable<any>((observer : Observer<any>) => {
      this.locateApi().subscribe(data => {
        let query = RestLocatorService.createUrl("config/:version/values", null);
        this.http.get<any>(this.endpointUrl + query, this.getRequestOptions())
          .subscribe(response => {
             this.setConfigValues(response.body.current);
             observer.next(response.body);
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
        this.http.get<any>(this.endpointUrl + query, this.getRequestOptions("application/json"))
          .subscribe((response) => {
            observer.next(response.body.current);
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
            .subscribe((response:any) => {
            observer.next(response.body.current);
            observer.complete();
          },(error:any)=>{
            observer.error(error);
            observer.complete();
          });
      });
    });
  }
    public getLanguageDefaults(lang:string) : Observable<any>{
        return new Observable<any>((observer : Observer<any>) => {
            this.locateApi().subscribe(data => {
                let query = RestLocatorService.createUrl("config/:version/language/defaults", null);
                this.http.get(this.endpointUrl + query, this.getRequestOptions("application/json",null,null,lang))
                    .subscribe(response => {
                        observer.next(response.body);
                        observer.complete();
                    },(error:any)=>{
                        observer.error(error);
                        observer.complete();
                    });
            });
        });
    }
    public getRequestOptions(contentType="application/json",username:string = null,password:string = null,locale=this.bridge.getISOLanguage()):
        {headers? : HttpHeaders,withCredentials:true, responseType: 'json',observe: 'response'}{
    let headers:any = {};
    if(contentType)
      headers['Content-Type']=contentType;
    headers['Accept']='application/json';
    if(locale)
        headers['locale']=locale;
    if(username!=null) {
        headers['Authorization'] ="Basic " + btoa(username + ":" + password);
    }
    else if(this.ticket!=null){
        headers['Authorization'] = "EDU-TICKET " + this.ticket;
        this.ticket=null;
    }
    else if(this.bridge.isRunningCordova() && this.bridge.getCordova().oauth!=null){
        headers['Authorization'] = "Bearer " + this.bridge.getCordova().oauth.access_token;
    }
    else{
        headers['Authorization'] = "";
    }
    if(this.bridge.isRunningCordova()){
        headers['DisableGuest']='true';
    }
    return {headers:headers,responseType: 'json',observe: 'response',withCredentials:true}; // Warn: withCredentials true will ignore a Bearer from OAuth!
  }
    private testEndpoint(url:string,local=true,observer:Observer<void>){
        this.http.get<any>(url+"_about", this.getRequestOptions())
            .subscribe((data)=> {
                    this._endpointUrl=url;
                    this._apiVersion=data.body.version.major+data.body.version.minor/10;
                    this.isLocating=false;
                    this.themesUrl=data.body.themesUrl;
                    console.log("API version "+this.apiVersion+" "+this._endpointUrl);
                    observer.next(null);
                    observer.complete();
                    return;
                },
                (error)=>{
                    if(error.status==RestConstants.HTTP_UNAUTHORIZED){
                        this._endpointUrl=url;
                        this.isLocating=false;
                        observer.next(null);
                        observer.complete();
                        return;
                    }
                    if(local==true){
                      this.testApi(false,observer);
                    }
                    else{
                      console.error("Could not contact rest api at location "+url);
                    }
                });
    }
    private testApi(local=true,observer : Observer<void>) : void{
      if(local) {
          let url = "rest/";
          this.testEndpoint(url,true,observer);
      }
      else{
          if(environment.production){
              console.error("Could not contact rest api. There is probably an issue with the backend");
              return;
          }
          else {
              this.http.get("assets/endpoint.txt",{responseType:'text'}).subscribe((data: any) => {
                  this.testEndpoint(data, false, observer);
              }, (error: any) => {
                  console.error("Could not contact locale rest endpoint and no url was found. Please create a file at assets/endpoint.txt and enter the url to your rest api",error);
              });
          }
      }
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
    return new Observable<void>((observer: Observer<void>) => {
      this.testApi(true,observer);
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

    getBridge() {
        return this.bridge;
    }
}
