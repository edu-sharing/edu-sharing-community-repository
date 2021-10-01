/* tslint:disable */
/* eslint-disable */
import { Node } from './node';
import { Serializable } from './serializable';
import { TrackingAuthority } from './tracking-authority';
export interface TrackingNode {
  authority?: TrackingAuthority;
  counts?: {
[key: string]: number;
};
  date?: string;
  fields?: {
[key: string]: Serializable;
};
  groups?: {
[key: string]: {
[key: string]: {
[key: string]: number;
};
};
};
  node?: Node;
}
