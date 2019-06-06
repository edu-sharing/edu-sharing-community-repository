import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {UIHelper} from "../../../core-ui-module/ui-helper"
@Component({
  selector: 'input-password',
  templateUrl: 'input-password.component.html',
  styleUrls: ['input-password.component.scss'],
})

export class InputPasswordComponent{
    public input_type = "password";
    @Input() id="passwordInput";
    @Input() required=false;
    @Input() autocomplete:string;
    @Input() inputClass="";
    @Input() hint=false;
    @Input() displayStrength=false;
    @Output() valueChange = new EventEmitter();
    @Input() placeholder="";
    @Output() change = new EventEmitter();
    @Output() keydown = new EventEmitter();
    @Output() keyup = new EventEmitter();
    @Output() ngModelChange = new EventEmitter();
    @ViewChild('input') nativeInput : ElementRef;
    passwordStrength: string;
    _value:string;
    @Input() public set value(_value:string){
      this._value=_value;
      this.valueChange.emit(_value);
      this.passwordStrength = UIHelper.getPasswordStrengthString(_value);
    }
    public get value(){
      return this._value;
    }
    public showPassword(){
        if (this.input_type === "password") {
            this.input_type = "text";
        } else {
            this.input_type = "password";
        }
    }

}
