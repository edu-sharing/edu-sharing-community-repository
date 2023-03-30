import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Node } from '../core-module/core.module';

/**
 * An application-wide event broker.
 */
@Injectable({
    providedIn: 'root',
})
export class LocalEventsService {
    readonly nodesChanged = new Subject<Node[]>();
    readonly nodesDeleted = new Subject<Node[]>();
}
