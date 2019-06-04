import {PipeTransform, Pipe} from '@angular/core';

@Pipe({name: 'debugResult'})
export class DebugResultPipe implements PipeTransform {
  transform(value : any,args:string[]): string {
    if (!value)
      return "No result";

    let data : any[];
    for (var property in value) {
      if(value.hasOwnProperty(property)){
        if(value[property].length) {
          data = value[property];
          break;
        }
      }
    }
    let result="";
    if (data && data.length){
      result = "Number of Results: " + data.length + "<br><br>";
      /*for (let obj of data) {
          result += "Item " + JSON.stringify(obj) + "<br>";
      }*/
      result+=JSON.stringify(value);
  }
    else{
      result=JSON.stringify(value);
    }
    result+="<hr>";
    return result;
  }
}
