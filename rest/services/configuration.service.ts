import {Injectable} from "@angular/core";
import {Observable, Observer} from "rxjs";
import {RestLocatorService} from "./rest-locator.service";
import {BridgeService, MessageType} from "../../../core-bridge-module/bridge.service";

/**
 Service to get configuration data while running (e.g. loaded extension)
 */
@Injectable()
export class ConfigurationService {
  private data : any=null;

  constructor(private bridge:BridgeService,private locator : RestLocatorService) {
    //this.getAll().subscribe(()=>{});
  }
  public getLocator(){
    return this.locator;
  }

  /**
   * Gets the whole configuration
   * @returns {any}
   */
  public getAll() : Observable<any>{
    return Observable.create( (observer:Observer<any>) => {
      if(this.data) {
        observer.next(this.data);
        observer.complete();
        return;
      }
      //this.http.get("assets/config.json").map((response: Response) => response.json()).subscribe((data:any)=>{
      this.locator.getConfig().subscribe((data)=>{
        this.data=data.current;
        observer.next(this.data);
        observer.complete();
      },(error)=>{
        // no language available, so use a fixed string
        this.bridge.showTemporaryMessage(MessageType.error, error,'Error fetching configuration data. Please contact administrator.<br />Fehler beim Abrufen der Konfigurationsdaten. Bitte Administrator kontaktieren.');
        console.warn(error)
      });
    });
  }
  public getDynamic(key:string,name:string,defaultValue:any=null){
      return Observable.create( (observer:Observer<any>) => {
          this.locator.getConfigDynamic(key).subscribe((data)=>{
              observer.next(this.instantInternal(name,defaultValue,data));
              observer.complete();
          },(error)=>{
              observer.error(error);
              observer.complete();
          });
      });
  }
  /**
   * Gets a value
   * Example: config.get("extension").subscribe((data)=>console.log(data));
   * Cascaded values can be also resolved by using a "." for seperation
   * E.g. rendering.showMetadata
   * @param name
   * @param defaultValue
   * @returns {any}
   */
  public get(name : string,defaultValue:any = null) : Observable<any>{
    return Observable.create( (observer:Observer<any>) => {
      if(this.data) {
        observer.next(this.instant(name,defaultValue));
        observer.complete();
        return;
      }
      this.getAll().subscribe((data:any)=>{
        observer.next(this.instant(name,defaultValue));
        observer.complete();
      });
    });
  }
  public instantInternal(name:string,defaultValue:any=null,object:any=this.data) : any{
    if(!object)
      return defaultValue;
    let parts=name.split(".");
    if(parts.length>1){
      if(object[parts[0]]) {
        let joined=name.substr(parts[0].length+1);
        return this.instantInternal(joined, defaultValue,object[parts[0]]);
      }
      else{
        return defaultValue;
      }
    }
    if (object[name] != null)
      return object[name];
    return defaultValue;
  }
  public instant(name:string,defaultValue:any=null) : any {
    return this.instantInternal(name, defaultValue);
  }
}
