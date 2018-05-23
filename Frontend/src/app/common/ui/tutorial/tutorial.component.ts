import {Component, ElementRef, Input, OnInit, Output} from '@angular/core';
import EventEmitter = NodeJS.EventEmitter;

@Component({
  selector: 'tutorial',
  templateUrl: 'tutorial.component.html',
  styleUrls: ['tutorial.component.scss']
})
export class TutorialComponent implements OnInit {
  @Input() element : ElementRef;
  @Output() onNext = new EventEmitter();
  @Output() onCancel = new EventEmitter();
  constructor() { }

  ngOnInit() {
  }

}
