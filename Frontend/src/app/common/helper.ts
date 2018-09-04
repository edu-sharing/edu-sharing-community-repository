/**
 * Created by shippeli on 23.02.2017.
 */
export class Helper {
  /**
   * Search the a property of the array array[index][property]==needle
   * Returns the index or -1 if no match was found
   * @param haystack
   * @param property
   * @param needle
   * @returns {number}
   */
  public static indexOfObjectArray(haystack: any, property: string, needle: any): number {
    for(let i = 0; i<haystack.length; i++) {
      if (haystack[i][property] == needle)
        return i;
    }
    return -1;
  }
    /**
     * Filter only all elements in the array where the given property matches the given needle
     * @param haystack
     * @param property
     * @param needle
     * @returns {number}
     */
    public static filterArray(haystack: any, property: string, needle: any): any {
      let result=[];
      for(let i = 0; i<haystack.length; i++) {
          if (haystack[i][property] == needle)
            result.push(haystack[i]);
      }
      return result;
    }

  /**
   * Returns true if both arrays are equal(same length, and all primitive objects are equal)
   * @param array1
   * @param array2
   * @returns {boolean}
   */
  public static arrayEquals(array1:any[],array2:any[]){
    if(array1==null)
      return array2==null;
    if(array2==null)
      return array1==null;
    if(array1.length!=array2.length)
      return false;
    for(let i=0;i<array1.length;i++){
      if(array1[i]!=array2[i])
        return false;
    }
    return true;
  }
    /**
     * Returns true if both objects have the same values stored
     * will not work for classes including methods or similar dynamic data
     * @param object1
     * @param object2
     * @returns {boolean}
     */
    public static objectEquals(object1:any,object2:any){
        if(object1==null)
            return object2==null;
        if(object2==null)
            return object1==null;
        return JSON.stringify(object1)==JSON.stringify(object2);
    }

  /**
   * Converts a date to a Year-Month-day string
   * @param date
   * @returns {string}
   */
  public static dateToString(date:Date) : string{
    let day=date.getDate()+"";
    if(day.length<2) day="0"+day;
    let month=(date.getMonth()+1)+"";
    if(month.length<2) month="0"+month;

    return date.getFullYear()+"-"+month+"-"+day;

  }

  /**
   * Like the regular array.indexof, but incase-sensitive
   * @param haystack
   * @param needle
   * @returns {number}
   */
  public static indexOfNoCase(haystack: string[],needle: string): number {
    if(!haystack)
      return -1;
    let i=0;
    for(let s of haystack){
      if(s.toLowerCase()==needle.toLowerCase())
        return i;
      i++;
    }
    return -1;
  }

  /**
   * Download a string as a text-data file
   * @param name Filename
   * @param data The string data to download
   */
  static downloadContent(name:string,data: string) {
    let dataPath = 'data:text/plain;charset=utf-8,'+encodeURIComponent(data);
    let a:any = document.createElement('A');
    a.href = dataPath;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  static arraySwap(array: any[], x: number, y: number) {
    if(x==y)
      return;
    var b = array[y];
    array[y] = array[x];
    array[x] = b;
  }

  /**
   *   add a get parameter to a given url
   */
  static addGetParameter(param: string, value: string, url: string) {

    if(url.indexOf("?")==-1)
      url+="?";
    else
      url+="&";

    return url+param+"="+encodeURIComponent(value);
  }

  public static deepCopy(data: any) {
    if(data==null)
      return null;
    return JSON.parse(JSON.stringify(data));
  }
  public static deepCopyArray(data: any[]) {
    if(!Array.isArray(data))
      return data;
    return data.slice();
  }

  /**
   * init an array with a given length and all values set to the init value
   * @param {number} length
   * @param value
   */
  static initArray(length: number, value: any=null) {
    let array:any=[];
    for(let i=0;i<length;i++){
      array.push(value);
    }
    return array;
  }
    static base64toBlob(base64:string,mimetype:string) {
        let sliceSize =  512;

        let byteCharacters = atob(base64);
        let byteArrays = [];

        for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
            let slice = byteCharacters.slice(offset, offset + sliceSize);

            let byteNumbers = new Array(slice.length);
            for (let i = 0; i < slice.length; i++) {
                byteNumbers[i] = slice.charCodeAt(i);
            }

            let byteArray = new Uint8Array(byteNumbers);
            byteArrays.push(byteArray);
        }

        let blob = new Blob(byteArrays, {type: mimetype});
        return blob;
    }

    /**
     * Join 2 arrays via the keys and return 1 array containing all keys from both arrays
     * The array at pos 2 will have priority and override the keys in the first one
     * @param array1
     * @param array
     */
    static arrayJoin(array1: any, array2: any) {
        if(array1==null)
            return array2;
        if(array2==null)
            return array1;
        let array:any=Helper.deepCopyArray(array1);
        for(let key in array2){
            array[key]=array2[key];
        }
        return array;
    }
}
