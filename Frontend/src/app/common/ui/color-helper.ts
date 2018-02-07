export class ColorHelper{
  public static cssColorToRgb(color:string) : number[] {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color);
    return result ? [
      parseInt(result[1], 16),
      parseInt(result[2], 16),
      parseInt(result[3], 16)
    ] : null;
  }

  public static rgbToHex(rgb:number[]) : string {
    return "#" + ColorHelper.componentToHex(rgb[0]) + ColorHelper.componentToHex(rgb[1]) + ColorHelper.componentToHex(rgb[2]);
  }
  private static componentToHex(c:number) {
    var hex = Math.max(0,Math.min(Math.round(c),255)).toString(16);
    return hex.length == 1 ? "0" + hex : hex;
  }

}
