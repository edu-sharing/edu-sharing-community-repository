export class UIConstants{
  public static ROUTER_PREFIX="components/";
  // also declared in scss!
  public static MOBILE_WIDTH = 700;
  public static MOBILE_HEIGHT = 750;
  public static MOBILE_STAGE = 100;
  public static MOBILE_TAB_SWITCH_WIDTH = UIConstants.MOBILE_WIDTH + UIConstants.MOBILE_STAGE*2;
  public static MEDIA_QUERY_MIN_WIDTH="min-width";
  public static MEDIA_QUERY_MAX_WIDTH="max-width";
  public static MEDIA_QUERY_MIN_HEIGHT="min-height";
  public static MEDIA_QUERY_MAX_HEIGHT="max-height";

}
export enum OPEN_URL_MODE{
    Current, // Current Window, or In App browser on cordova
    Blank, // New Window, or In App browser on cordova
    BlankSystemBrowser, // New Window, or system browser on cordova
};
