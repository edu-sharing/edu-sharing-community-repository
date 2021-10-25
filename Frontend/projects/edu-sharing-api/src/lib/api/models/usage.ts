/* tslint:disable */
/* eslint-disable */
import { Parameters } from './parameters';
export interface Usage {
    appId: string;
    appSubtype?: string;
    appType?: string;
    appUser: string;
    appUserMail: string;
    courseId: string;
    created?: string;
    distinctPersons?: number;
    fromUsed?: string;
    guid?: string;
    modified?: string;
    nodeId: string;
    parentNodeId: string;
    resourceId: string;
    toUsed?: string;
    type?: string;
    usageCounter?: number;
    usageVersion: string;
    usageXmlParams?: Parameters;
    usageXmlParamsRaw?: string;
}
