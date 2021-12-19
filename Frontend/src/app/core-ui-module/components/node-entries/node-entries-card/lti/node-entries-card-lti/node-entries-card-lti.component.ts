import {Component, Input, OnInit} from '@angular/core';
import {Node} from '../../../../../../core-module/rest/data-object';

@Component({
  selector: 'es-node-entries-card-lti',
  templateUrl: './node-entries-card-lti.component.html',
  styleUrls: ['./node-entries-card-lti.component.scss']
})
export class NodeEntriesCardLtiComponent<T extends Node> implements OnInit {


    @Input() node: T;


  constructor() { }

  ngOnInit(): void {
  }

}
