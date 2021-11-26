import { Component, Input, OnInit } from '@angular/core';
import { OptionItem } from '../../option-item';

@Component({
    selector: 'es-list-option-item',
    templateUrl: './list-option-item.component.html',
    styleUrls: ['./list-option-item.component.scss'],
})
export class ListOptionItemComponent implements OnInit {
    @Input() option: OptionItem;

    constructor() {}

    ngOnInit(): void {}
}
