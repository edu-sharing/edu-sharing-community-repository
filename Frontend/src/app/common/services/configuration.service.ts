

import {Injectable} from "@angular/core";
import {Http, Response} from "@angular/http";
import {Observer, Observable} from "rxjs";
/**
 Service to get configuration data while running (e.g. loaded extension)
 */
@Injectable()
export class ConfigurationService {
  private data : any=null;

  constructor(private http : Http) {
    this.getAll().subscribe(()=>{});
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
      this.http.get("assets/config.json").map((response: Response) => response.json()).subscribe((data:any)=>{
        this.data=data;
        observer.next(this.data);
        observer.complete();
      },(error:any)=>console.warn(error));
    });
  }

  /**
   * Gets a value
   * Example: config.get("extension").subscribe((data)=>console.log(data));
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
  public instant(name:string,defaultValue:any=null) : any{
    if(!this.data)
      return null;
    if (this.data[name] != null)
      return this.data[name];
    return defaultValue;
  }
}
