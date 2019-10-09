
import {TranslateService} from "@ngx-translate/core";
import {isNumeric} from "rxjs/util/isNumeric";
import {Translation} from "../translation";

export class FormatOptions {
    showDate?=true;
    showAlwaysTime?=false;
    showSeconds?=false;
    useRelativeLabels?=true;
}

export class DateHelper{
  /**
   * Fill a date (day + month) string, e.g. 2 -> 02
   * @param date
   * @returns {string}
   */
  private static fillDate(date:number) : string{
    if(date<10)
      return "0"+date;
    return date+"";
  }

  /**
   * Convert date to a unix timestamp
   * @param date
   * @returns {number}
   */
  public static convertDate(date : any){
    return new Date(date).getTime();
  }

  /**
   * format a date with a given, fixed string
   * For consistency, we use the patterns from
   * https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
   * @param date
   * @param {string} format
   * @returns {string}
   */
  public static formatDateByPattern(date: number|any,format:string) : string{
    if(!isNumeric(date)) {
      return date;
    }
    let dateObject = new Date((date as number) * 1);
    format=format.replace("y",""+dateObject.getFullYear());
    format=format.replace("M",""+DateHelper.fillDate(dateObject.getMonth()));
    format=format.replace("d",""+DateHelper.fillDate(dateObject.getDate()));
    return format;
  }

  /**
   * Format a date for the current language
   * @param translation
   * @param date
   * @param showAlwaysTime
   * @returns {any}
   */
  public static formatDate(translation : TranslateService,date: number|any,options:FormatOptions = new FormatOptions()) : string{
    try {
      if(!isNumeric(date)) {
        return date;
      }
      let dateObject = new Date((date as number) * 1);
      let dateToday = new Date();
      let dateYesterday = new Date();
      dateYesterday.setDate(dateYesterday.getDate()-1);
      let isToday = dateObject.toDateString()==dateToday.toDateString();
      let isYesterday = dateObject.toDateString()==dateYesterday.toDateString();
      let prefix = "";
      let timeDiff = (dateToday.getTime()-dateObject.getTime())/1000;
      let diffDays = Math.ceil(timeDiff / (3600 * 24));
      let addDate=true;
      let timeFormat="HH:mm";
      if(options.showSeconds){
          timeFormat+=":ss";
      }
      if(timeDiff<3600*6 && options.useRelativeLabels){
        if(timeDiff<90){
          prefix=translation.instant("JUST_NOW",{seconds:timeDiff})
        }
        else if(timeDiff<60*100){
          prefix=translation.instant("MINUTES_AGO",{minutes:Math.round(timeDiff/60)})
        }
        else{
          prefix=translation.instant("HOURS_AGO",{hours:Math.round(timeDiff/(60*60))})
        }
        addDate=false;
        timeFormat="";
      }
      else if (isToday && options.useRelativeLabels) {
        prefix = translation.instant("TODAY");
        addDate = false;
      }
      else if(isYesterday && options.useRelativeLabels){
        prefix = translation.instant("YESTERDAY");
        addDate = false;
      }
      else if(diffDays<6 && options.useRelativeLabels){
        prefix = translation.instant("DAYS_AGO",{days:diffDays});
        addDate = false;
        if(!options.showAlwaysTime)
          timeFormat = "";
      }
      else{
        if(!options.showAlwaysTime)
          timeFormat="";
      }
      // ng2's dateformatter is super slow, but it doesn't matter, we just iterate it once :)
      //return dateFormat+time;
      let str=prefix;
      if(addDate) {
        if(Translation.getLanguage()=="en"){
          str += dateObject.getFullYear()+"-"+DateHelper.fillDate(dateObject.getMonth()+1)+"-"+DateHelper.fillDate(dateObject.getDate());
        }
        else {
          str += DateHelper.fillDate(dateObject.getDate()) + "." + DateHelper.fillDate(dateObject.getMonth() + 1) + "." + dateObject.getFullYear();
        }
        //str += DateFormatter.format(dateObject, Translation.getLanguage(), dateFormat).trim();
      }
      if(options.showDate==false){
          str="";
      }
      // ie fixes, timeFormat not working
      if(timeFormat){
        if(str)
          str+=", ";
        let timeValue=dateObject.toLocaleTimeString(Translation.getLanguage());
        let times=timeValue.split(":");
        str += timeFormat.replace("HH",times[0]).replace("mm",times[1]).replace("ss",times[2]);
      }
      return str;
      /*
      let dateString=prefix+" ";
      if(dateFormat!=""){
        dateString+=dateObject.toLocaleDateString(Translation.getLanguage())+" ";
      }
      if(time!=""){
        dateString+=dateObject.toLocaleTimeString(Translation.getLanguage());
      }
      return dateString;
      */
    }
    catch(e){
      return (date as any);
    }
  }
  static getDateFromDatepicker(date:Date){
    return new Date(date.getTime()-date.getTimezoneOffset()*60*1000);
  }

    static getDateForNewFile() {
        return DateHelper.formatDate(null,new Date().getTime(),{showAlwaysTime:true,useRelativeLabels:false})
    }
}
