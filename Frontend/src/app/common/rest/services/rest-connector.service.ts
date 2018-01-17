import { Injectable } from '@angular/core';
import {Headers, Http, RequestOptions, RequestOptionsArgs, Response} from "@angular/http";
import {RestConstants} from "../rest-constants";
import {RestHelper} from "../rest-helper";
import {Observable, Observer} from "rxjs";
import {RequestObject} from "../request-object";
import {environment} from "../../../../environments/environment";
import {OAuthResult, LoginResult, AccessScope} from "../data-object";
import {FrameEventsService} from "../../services/frame-events.service";
import {Router, ActivatedRoute} from "@angular/router";
import {TemporaryStorageService} from "../../services/temporary-storage.service";
import {UIConstants} from "../../ui/ui-constants";
import {ConfigurationService} from "../../services/configuration.service";
import {RestLocatorService} from "./rest-locator.service";

/**
 * The main connector. Manages the API Endpoint as well as common api parameters and url generation
 * Use this service to setup your REST Service Connection.
 * NO NOT USE this service to directly perform requests; Use the proper Rest Services for the endpoints instead
 */
@Injectable()
export class RestConnectorService {
  private static DEFAULT_NUMBER_PER_REQUEST = 25;
  private _lastActionTime=0;
  private _currentRequestCount=0;
  private _logoutTimeout: number;
  private _autoLogin = true;
  public _scope: string;
  private themesUrl: any;

  get autoLogin(): boolean {
    return this._autoLogin;
  }

  set scope(value: string) {
    this._scope = value;
  }
  get scope(): string {
    return this._scope;
  }

  set autoLogin(value: boolean) {
    console.log(value);
    this._autoLogin = value;
  }
  get endpointUrl(): string {
    return this.locator.endpointUrl;
  }
  get numberPerRequest(): number {
    return this.locator.numberPerRequest;
  }

  set numberPerRequest(value: number) {
    this.locator.numberPerRequest=value;
  }
  get lastActionTime(){
    return this._lastActionTime;
  }
  get logoutTimeout(){
    return this._logoutTimeout;
  }
  public getRequestOptions(contentType="application/json",username:string = null,password:string = null) : RequestOptionsArgs{
    return this.locator.getRequestOptions(contentType,username,password);
  }
  constructor(private router:Router,
              private http : Http,
              private config: ConfigurationService,
              private locator: RestLocatorService,
              private storage : TemporaryStorageService,
              private event:FrameEventsService) {
    this.numberPerRequest=RestConnectorService.DEFAULT_NUMBER_PER_REQUEST;
    event.addListener(this);
  }
  public onEvent(event:string,request:any){
    if(event==FrameEventsService.EVENT_UPDATE_SESSION_TIMEOUT) {
      this._lastActionTime=new Date().getTime();
    }
    if(event==FrameEventsService.EVENT_PARENT_REST_REQUEST){
      let method=request.method ? request.method.toLowerCase() : "get";
      let path=request.path;
      let body=request.body;
      if(method=='get'){
        this.get(path,this.getRequestOptions()).map((response: Response) => response.json()).subscribe((data:any)=>{
          this.notifyFrame(data,request,true);
        },(error:any)=>this.notifyFrame(error,request,false));
      }
    }
  }


  public getOAuthToken() : Observable<OAuthResult>{
  let url=this.createUrl("../oauth2/token",null);
  //"grant_type=password&client_id=eduApp&client_secret=secret&username=admin&password=admin"
  return new Observable<OAuthResult>((observer : Observer<OAuthResult>)=>{
    this.post(url,"client_id=eduApp&grant_type=client_credentials&client_secret=secret"
      //"&username="+encodeURIComponent(username)+
      //"&password="+encodeURIComponent(password)
      ,this.getRequestOptions("application/x-www-form-urlencoded")).map((response: Response) => response.json()).subscribe(
      (data:OAuthResult) => {
        observer.next(data);
        observer.complete();
      },
      (error:any) =>{
        observer.error(error);
        observer.complete();
      });
  });

}
  public logout() : Observable<Response>{
    let url=this.createUrl("authentication/:version/destroySession",null);
    this.event.broadcastEvent(FrameEventsService.EVENT_USER_LOGGED_OUT);
    return this.get(url,this.getRequestOptions());
  }
  public logoutSync() : any{
    let url=this.createUrl("authentication/:version/destroySession",null);
    let xhr = new XMLHttpRequest();
    let options=this.getRequestOptions("");
    xhr.withCredentials=options.withCredentials;
    xhr.open("GET",this.endpointUrl+url,false);
    let result=xhr.send();
    this.event.broadcastEvent(FrameEventsService.EVENT_USER_LOGGED_OUT);
    return result;
  }
  public getCurrentLogin() : LoginResult{
    return this.storage.get(TemporaryStorageService.SESSION_INFO);
  }
  public getConfig() : Observable<any>{
    let url=this.createUrl("config/:version/get",null);
    return this.http.get(url,this.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public isLoggedIn() : Observable<LoginResult>{
    let url=this.createUrl("authentication/:version/validateSession",null);
    return new Observable<LoginResult>((observer : Observer<LoginResult>)=>{
      this.get(url,this.getRequestOptions()).map((response: Response) => response.json()).subscribe(
        (data:LoginResult)=>{
          this.toolPermissions=data.toolPermissions;
          this.event.broadcastEvent(FrameEventsService.EVENT_UPDATE_LOGIN_STATE,data);
          this.storage.set(TemporaryStorageService.SESSION_INFO,data);
          this._logoutTimeout=data.sessionTimeout;
          observer.next(data);
          observer.complete();
        },
        (error:any)=>{
          observer.error(error);
          observer.complete();
        }
      );
    });
  }
  public hasAccessToScope(scope:string) : Observable<AccessScope>{
    let url=this.createUrl("authentication/:version/hasAccessToScope/?scope=:scope",null,[[":scope",scope]]);
    return this.get(url,this.getRequestOptions()).map((response: Response) => response.json());
  }
  private toolPermissions: string[];
  public hasToolPermissionInstant(permission:string){
    if(this.toolPermissions)
      return this.toolPermissions.indexOf(permission) != -1;
    return false;
  }
  public hasToolPermission(permission:string){
    console.log(this.toolPermissions);
    return new Observable<boolean>((observer : Observer<boolean>) => {
      if (this.toolPermissions == null) {
        this.isLoggedIn().subscribe(() => {
          observer.next(this.hasToolPermissionInstant(permission));
          observer.complete();
        }, (error: any) => observer.error(error));
      }
      else{
        observer.next(this.hasToolPermissionInstant(permission));
        observer.complete();
      }
    });
  }
  public login(username:string,password:string,scope:string=null) : Observable<string>{

    let url = this.createUrl("authentication/:version/validateSession", null);
    if(scope) {
      url = this.createUrl("authentication/:version/loginToScope", null);
    }
    return new Observable<string>((observer : Observer<string>)=>{
      if(scope){
        this.post(url,JSON.stringify({
          userName:username,
          password:password,
          scope:scope
        }),this.getRequestOptions()).map((response: Response) => response.json()).subscribe(
          (data:LoginResult) => {
            if(data.isValidLogin)
              this.event.broadcastEvent(FrameEventsService.EVENT_USER_LOGGED_IN,data);
            this.storage.set(TemporaryStorageService.SESSION_INFO,data);
            observer.next(data.statusCode);
            observer.complete();
          },
          (error:any) =>{
            console.log(error);
            observer.error(error);
            observer.complete();
          });
      }
      else {
        this.get(url, this.getRequestOptions("",username,password)).map((response: Response) => response.json()).subscribe(
          (data: LoginResult) => {
            if(data.isValidLogin)
              this.event.broadcastEvent(FrameEventsService.EVENT_USER_LOGGED_IN,data);
            this.storage.set(TemporaryStorageService.SESSION_INFO,data);
            observer.next(data.statusCode);
            observer.complete();
          },
          (error: any) => {
            console.log(error);

            observer.error(error);
            observer.complete();
          });
      }
    });

  }
  public createRequestString(request : RequestObject){
    let str="skipCount="+(request && request.offset ? request.offset : 0)+
      "&maxItems="+(request && request.count!=null ?  request.count : this.numberPerRequest);
    if(request==null)
      return str;
    str+="&"+RestHelper.getQueryString("sortProperties",request && request.sortBy!=null ? request.sortBy : RestConstants.DEFAULT_SORT_CRITERIA);

    if(request.sortAscending!=null && request.sortAscending.length>1)
      str+="&"+RestHelper.getQueryString("sortAscending",request.sortAscending);
    else
      str+="&sortAscending="+(request && request.sortAscending!=null ? request.sortAscending : RestConstants.DEFAULT_SORT_ASCENDING);

    str+="&"+RestHelper.getQueryString("propertyFilter",request && request.propertyFilter!=null ? request.propertyFilter : []);
    return str;
  }
  /**
   * Replaces jokers inside the url and espaces them. The default joker :version and :repository is always replaced!
   * @param url
   * @param repository the repo name
   * @param urlParams An array of params First value is the search joker, second the replace value
   * The search value may ends with |noescape. E.g. :sort|noescape. This tells the method to not escape the value content
   * @returns {string}
   */
  public createUrl(url : string,repository : string,urlParams : string[][] = []) {
    return RestLocatorService.createUrl(url,repository,urlParams);
  }


  /**
   * Same as createUrl, but does not escape the params. Escaping needs to be done when calling the method
   * @param url
   * @param repository
   * @param urlParams
   * @returns {string}
   */
  public createUrlNoEscape(url : string,repository : string,urlParams : string[][] = []) {
    return RestLocatorService.createUrlNoEscape(url,repository,urlParams);
  }

  public sendDataViaXHR(url : string,file : File,method='POST',fieldName='file') : Observable<XMLHttpRequest>{
    return Observable.create( (observer:Observer<XMLHttpRequest>) => {
      try {
        var xhr: XMLHttpRequest = new XMLHttpRequest();
        xhr.onreadystatechange = () => {
          if (xhr.readyState === 4) {
            if (xhr.status === 200) {
              observer.next(xhr);
              observer.complete();
            } else {
              console.error(xhr);
              observer.error(xhr);
            }
          }
        };
        let options=this.getRequestOptions("");
        xhr.withCredentials=options.withCredentials;
        xhr.open(method, this.endpointUrl+url, true);
        for (let key of options.headers.keys()) {
           xhr.setRequestHeader(key, options.headers.get(key));
        }
        let formData = new FormData();
        formData.append(fieldName, file, file.name);
        xhr.send(formData);
        console.log("xhr send");
      }catch(e){
        console.error(e);
        observer.error(e);
      }
    });
  }

  public get(url:string,options:RequestOptionsArgs,appendUrl=true) : Observable<Response>{
    return new Observable<Response>((observer : Observer<Response>) => {
      this.locator.locateApi().subscribe(data => {
        this._lastActionTime=new Date().getTime();
        this._currentRequestCount++;
        this.http.get((appendUrl ? this.endpointUrl : '') + url, options).subscribe(response => {
            this._currentRequestCount--;
            this.checkHeaders(response);
            observer.next(response);
            observer.complete();
        },
        error => {
          this._currentRequestCount--;
          this.checkLogin(error);
          observer.error(error);
          observer.complete();
        });
      });
    });
  }
  public post(url:string,body : any,options:RequestOptionsArgs) : Observable<Response>{
    return new Observable<Response>((observer : Observer<Response>) => {
      this.locator.locateApi().subscribe(data => {
        this._lastActionTime=new Date().getTime();
        this._currentRequestCount++;
        this.http.post(this.endpointUrl + url,body, options).subscribe(response => {
            this._currentRequestCount--;
            this.checkHeaders(response);
            observer.next(response);
            observer.complete();
          },
        error => {
          this._currentRequestCount--;
          this.checkLogin(error);
          observer.error(error);
          observer.complete();
        })
      });
    });
  }
  public put(url:string,body : any,options:RequestOptionsArgs) : Observable<Response>{
    return new Observable<Response>((observer : Observer<Response>) => {
      this.locator.locateApi().subscribe(data => {
        this._lastActionTime=new Date().getTime();
        this._currentRequestCount++;
        this.http.put(this.endpointUrl + url,body, options).subscribe(response => {
            this._currentRequestCount--;
            this.checkHeaders(response);
            observer.next(response);
            observer.complete();
        },
          error => {
            this._currentRequestCount--;
            this.checkLogin(error);
            observer.error(error);
            observer.complete();
          });
      });
    });
  }
  public delete(url:string,options:RequestOptionsArgs) : Observable<Response>{
    return new Observable<Response>((observer : Observer<Response>) => {
      this.locator.locateApi().subscribe(data => {
        this._lastActionTime=new Date().getTime();
        this._currentRequestCount++;
        this.http.delete(this.endpointUrl + url, options).subscribe(response => {
            this._currentRequestCount--;
            this.checkHeaders(response);
            observer.next(response);
            observer.complete();
        },
          error => {
            this._currentRequestCount--;
            this.checkLogin(error);
            observer.error(error);
            observer.complete();
          });
      });
    });
  }

  /**
   * returns how much requests are currently not answered (waiting for response)
   */
  public getCurrentRequestCount(){
    return this._currentRequestCount;
  }

  /**
   * Fires the observer as soon as all requests are done
   * @returns {Observable<void>|"../../../Observable".Observable<void>|"../../Observable".Observable<void>}
   */
  public onAllRequestsReady() : Observable<void>{
    return new Observable<void>((observer : Observer<void>) => {
      this.onAllRequestsReadyObserver(observer);
    });
  }
  public onAllRequestsReadyObserver(observer:Observer<void>){
    setTimeout(()=>{
      if(this._currentRequestCount>0){
        this.onAllRequestsReadyObserver(observer);
      }
      else{
        observer.next(null);
        observer.complete();
      }
    },50);
  }

  /**
   * Returns the current api version (usually a value > 1, can be floating point), or -1 if no api is connected
   * @returns {number}
   */
  public getApiVersion(){
    return this.locator.apiVersion;
  }

  /**
   * Returns the absolute url to the current rest endpoint
   * @returns {string}
   */
  public getAbsoluteEndpointUrl() {
    if(this.endpointUrl.toLowerCase().startsWith("http://") || this.endpointUrl.toLowerCase().startsWith("https://"))
      return this.endpointUrl;
    let baseURL = location.protocol + "//" + location.hostname + (location.port && ":" + location.port);
    if (document.getElementsByTagName('base').length > 0) {
      baseURL = document.getElementsByTagName('base')[0].href;
    }
    if(!baseURL.endsWith("/"))
      baseURL+="/";
    return baseURL+this.endpointUrl;
  }

  /**
   * Check if login is invalid and redirect to login page
   * @param error
   * @returns {boolean} true if not logged in and redirected
   */
  private checkLogin(error: any) {
    if (!this._autoLogin)
      return false;
    if (error.status == RestConstants.HTTP_UNAUTHORIZED) {
      this.goToLogin();
      return true;
    }
    return false;
  }

  private notifyFrame(data: any,request:any, success : boolean) {
    let result={request:request,response:data,success:success};
    console.log(result);
    this.event.broadcastEvent(FrameEventsService.EVENT_REST_RESPONSE,result);
  }

  public setRoute(route: ActivatedRoute) {
    return this.locator.setRoute(route);
  }

  private checkHeaders(response: Response) {
    if(!this._scope)
      return;
    if(this._scope!=response.headers.get('X-Edu-Scope')){
      this.goToLogin(null);
    }
  }
  private goToLogin(scope=this._scope) {
    if(this.currentPageIsLogin())
      return;
    RestHelper.goToLogin(this.router,this.config,scope);
    //this.router.navigate([UIConstants.ROUTER_PREFIX+"login"],{queryParams:{scope:scope?scope:"",next:window.location}});
  }

  private currentPageIsLogin() {
    return window.location.href.indexOf("components/login")!=-1;
  }
  public getThemeMimeIconSvg(name:string){
    return this.themesUrl+"images/common/mime-types/svg/"+name;
  }
  public getThemeMimePreview(name:string){
    return this.themesUrl+"images/common/mime-types/previews/"+name;
  }


}
