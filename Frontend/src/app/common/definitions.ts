import {Node as NodeLegacy} from '../core-module/rest/data-object';
import { Node } from 'ngx-edu-sharing-api';

/**
 * universal node object which can be from the new/mapped API or legacy defined data-object
 */
export type UniversalNode = (NodeLegacy | Node);
