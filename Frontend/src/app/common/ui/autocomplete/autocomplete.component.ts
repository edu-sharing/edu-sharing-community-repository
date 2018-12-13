import {Component, Input, Output, EventEmitter, OnInit, SimpleChanges} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'autocomplete',
  templateUrl: 'autocomplete.component.html',
  styleUrls: ['autocomplete.component.scss'],
  host: {'(keydown)': 'handleKeyDown($event)',
  '(keyup)': 'handleKeyUp($event)'}
})


export class AutocompleteComponent{

  @Input() id: string;
  @Input() placeholder: string;
  @Input() hintBottom: string;
  @Input() inputMinLength: number = 2;
  @Input() disabled=false;
  /**
   * Do allow any entered value to be accepted
   * @type {boolean}
   */
  @Input() allowAny=false;

  private _fixed=false;
    /**
     * should the suggestions be fixed (basically all results are always shown, never reset
     * If active, updateInput() is not called
     */
  @Input() set fixed(fixed:boolean){
      this._fixed=fixed;
      if(fixed){
          this.maxSuggestions=9999999;
          this.inputMinLength=0;
      }
    }
    @Input() maxSuggestions: number = 10;

  @Input() set suggestions(suggestions: SuggestItem[]) {
      this._suggestions = [];
      if(typeof suggestions != 'undefined') {
        for(var i = 0, j = 0; i<suggestions.length && i<this.maxSuggestions; i++) {
          var suggestItem = suggestions[i];
          if(!this.itemChosen(suggestItem)) {
            suggestItem.index = j;
            j++;
            this._suggestions.push(suggestItem);
          }
      }
    }
  }

  @Input() chosen: SuggestItem[];

  @Output() updateInput: EventEmitter<any> = new EventEmitter<any>();
  @Output() addItem: EventEmitter<any> = new EventEmitter<any>();
  @Output() addAny: EventEmitter<any> = new EventEmitter<any>();
  @Output() removeItem: EventEmitter<any> = new EventEmitter<any>();

  _suggestions: SuggestItem[];
  valueInput: string = '';
  showSuggestions: boolean = false;
  activeItem: number = -1;

  constructor(private translate : TranslateService) {}

addValue(){
    if(this.allowAny && this.valueInput){
      this.addAny.emit(this.valueInput);
    }
}
itemChosen(item:SuggestItem) {
  if(typeof this.chosen != 'undefined') {
    for(var i = 0; i< this.chosen.length;i++) {
      if(this.chosen[i].title == item.title)
        return true;
    }
  }
  return false;
}

  handleSuggestClick(item:SuggestItem) {
    this.addItem.emit({item:item, id:this.id});
    this.clear();
  }

  removeValue(item:SuggestItem) {
    this.removeItem.emit({item:item, id:this.id});
  }

  clear() {
    if(!this._fixed) {
        this._suggestions = [];
    }
    this.valueInput = '';
    this.activeItem = -1;
    this.showSuggestions=false;
  }

  handleKeyUp(event: any) {
    if(this.valueInput.length >= this.inputMinLength) {
      this.updateInput.emit({input: this.valueInput, id: this.id});
      this.showSuggestions = true;
     } else {
       this.showSuggestions = false;
       if(!this._fixed) {
           this._suggestions = [];
       }
       this.activeItem = -1;
     }
  }

  handleKeyDown(event: any) {
      if ((event.keyCode == 40 || event.keyCode == 38 || event.keyCode == 13) && this.showSuggestions) {
        event.preventDefault();

        if (event.keyCode == 40) {
          this.activeItem++;
          if(this.activeItem >= this._suggestions.length)
            this.activeItem = 0;
        }

        if (event.keyCode == 38) {
              this.activeItem--;
              if(this.activeItem < 0)
                this.activeItem = this._suggestions.length - 1;
        }

        if (event.keyCode == 13) {
          if(this.activeItem > -1) {
            for(var i = 0; i < this._suggestions.length; i++) {
              if(this._suggestions[i].index == this.activeItem)
                var item = this._suggestions[i];
            }
            if(item) {
              this.addItem.emit({item:item, id:this.id});
              this.clear();
              event.stopPropagation();
            }
          }
        }
    }
  }
}

export class SuggestItem {
  public index: number;
  public originalObject : any;
  public secondaryTitle: string;
  constructor(public id: string, public title: string, public materialIcon: string = null, public iconUrl: string = null, public key?: string = null) {
  }

}
