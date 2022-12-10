import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Node } from 'ngx-edu-sharing-api';

@Injectable({
    providedIn: 'root',
})
export class BreadcrumbsService {
    breadcrumbs$ = new BehaviorSubject<Node[]>([]);

    setNodePath(path: Node[]) {
        this.breadcrumbs$.next(path);
    }
}
