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
  @Input() inputMinLength: number = 2;
  @Input() disabled=false;
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
  @Output() removeItem: EventEmitter<any> = new EventEmitter<any>();

  _suggestions: SuggestItem[];
  valueInput: string = '';
  showSuggestions: boolean = false;
  activeItem: number = -1;

  constructor(private translate : TranslateService) {}


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
    this._suggestions = [];
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
       this._suggestions = [];
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
            }
          }
        }
    }
  }
}

export class SuggestItem {
  public index: number;
  public originalObject : any;
  constructor(public id: string, public title: string, public materialIcon: string, public iconUrl: string, public key?: string) {
  }

}
