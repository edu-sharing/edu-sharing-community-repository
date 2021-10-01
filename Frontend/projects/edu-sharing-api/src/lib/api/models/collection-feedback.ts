/* tslint:disable */
/* eslint-disable */
import { Serializable } from './serializable';
export interface CollectionFeedback {
  createdAt?: string;
  creator?: string;
  feedback?: {
[key: string]: Serializable;
};
}
