/* tslint:disable */
/* eslint-disable */
import { Serializable } from './serializable';
import { TrackingAuthority } from './tracking-authority';
export interface Tracking {
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
}
