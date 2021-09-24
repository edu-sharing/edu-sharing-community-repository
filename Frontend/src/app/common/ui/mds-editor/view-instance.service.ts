import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable()
export class ViewInstanceService {
    readonly isExpanded$ = new BehaviorSubject(true);
}
