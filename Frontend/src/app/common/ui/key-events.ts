
export class KeyEvents{
  public static eventFromInputField(event:any) {
    return event.srcElement && (event.srcElement.minLength || event.srcElement.maxLength);
  }
}
