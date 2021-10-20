/* tslint:disable */
/* eslint-disable */
import { Catalog } from './catalog';
export interface MediacenterProfileExtension {
    catalogs?: Array<Catalog>;
    contentStatus?: 'Activated' | 'Deactivated';
    districtAbbreviation?: string;
    id?: string;
    location?: string;
    mainUrl?: string;
}
