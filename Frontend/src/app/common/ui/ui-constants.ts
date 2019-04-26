export class UIConstants{
  public static ROUTER_PREFIX="components/";
  // also declared in scss!
  public static MOBILE_WIDTH = 600;
  public static MOBILE_STAGE = 100;

}
export enum OPEN_URL_MODE{
    Current, // Current Window, or In App browser on cordova
    Blank, // New Window, or In App browser on cordova
    BlankSystemBrowser, // New Window, or system browser on cordova
};