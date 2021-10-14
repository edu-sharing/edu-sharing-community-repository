/* tslint:disable */
/* eslint-disable */
import { ServiceInstance } from './service-instance';
export interface Service {
    instances: Array<ServiceInstance>;
    name: string;
}
