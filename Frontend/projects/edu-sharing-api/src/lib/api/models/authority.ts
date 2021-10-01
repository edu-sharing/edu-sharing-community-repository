/* tslint:disable */
/* eslint-disable */
export interface Authority {
  authorityName: string;
  authorityType?: 'USER' | 'GROUP' | 'OWNER' | 'EVERYONE' | 'GUEST';
  properties?: {
[key: string]: Array<string>;
};
}
