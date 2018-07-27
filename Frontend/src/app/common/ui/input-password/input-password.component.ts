import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";
@Component({
  selector: 'input-password',
  templateUrl: 'input-password.component.html',
  styleUrls: ['input-password.component.scss'],
})

export class InputPasswordComponent{
    public input_type = "password";
    @Input() id="passwordInput";
    @Input() required=false;
    @Input() value:string;
    @Output() valueChange = new EventEmitter();
    @Input() placeholder="";
    @Output() change = new EventEmitter();
    @Output() keydown = new EventEmitter();
    @Output() keyup = new EventEmitter();
    @ViewChild('input') nativeInput : ElementRef;
    public set _value(_value:string){
      this.value=_value;
      this.valueChange.emit(_value);
    }
    public get _value(){
      return this.value;
    }
    public showPassword(){
        if (this.input_type === "password") {
            this.input_type = "text";
        } else {
            this.input_type = "password";
        }
    }

}
