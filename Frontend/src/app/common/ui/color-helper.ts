export class ColorHelper{
  public static BRIGHTNESS_THRESHOLD_COLLECTIONS = 0.6;
  public static cssColorToRgb(color:string) : number[] {
    color=color.trim();
    if(color.startsWith("rgb")){
      console.log(color);
      let result = /rgb.?\(\s*([\d]*)\s*,\s*([\d]*)\s*,\s*([\d*]*)\s*/i.exec(color);
      return result ? [
        parseInt(result[1]),
        parseInt(result[2]),
        parseInt(result[3])
      ] : null;
    }
    let result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color);
    return result ? [
      parseInt(result[1], 16),
      parseInt(result[2], 16),
      parseInt(result[3], 16)
    ] : null;
  }

  /**
   * returns the estimated color brightness for a css color based on a pereceived brightness factor
   * Value between [0,1] and -1 if the css string was not readable
   * @param {string} color
   * @returns {number}
   */
  public static getColorBrightness(color:string){
    let rgb=ColorHelper.cssColorToRgb(color);
    if(rgb){
      return (rgb[0]*0.2126 + rgb[1]*0.7152 + rgb[2]*0.0722) / 255;
    }
    return -1;
  }

  public static rgbToHex(rgb:number[]) : string {
    return "#" + ColorHelper.componentToHex(rgb[0]) + ColorHelper.componentToHex(rgb[1]) + ColorHelper.componentToHex(rgb[2]);
  }
  private static componentToHex(c:number) {
    var hex = Math.max(0,Math.min(Math.round(c),255)).toString(16);
    return hex.length == 1 ? "0" + hex : hex;
  }

}
