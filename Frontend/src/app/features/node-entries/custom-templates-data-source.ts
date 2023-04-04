import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { GenericAuthority, Pagination, Node } from 'src/app/core-module/core.module';
import { ItemsCap } from './items-cap';
import { Helper } from '../../core-module/rest/helper';
import { NodeDataSource } from './node-data-source';

/**
 * this is a special data source to provide custom card layouts into the node-entries components
 */
export class CustomTemplatesDataSource extends NodeDataSource<any> {}
