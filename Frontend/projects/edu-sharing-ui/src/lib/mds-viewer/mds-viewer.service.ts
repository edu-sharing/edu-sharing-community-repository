import { ElementRef, Injectable, QueryList, ViewChildren } from '@angular/core';
import { MdsDefinition } from 'ngx-edu-sharing-api';
import { BehaviorSubject } from 'rxjs';
import { Values } from '../services/search-helper.service';

@Injectable()
export class MdsViewerService {
    @ViewChildren('container') container: QueryList<ElementRef>;
    values$ = new BehaviorSubject<Values>(undefined);
    mds$ = new BehaviorSubject<MdsDefinition>(undefined);
}
