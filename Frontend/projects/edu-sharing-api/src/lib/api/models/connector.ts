/* tslint:disable */
/* eslint-disable */
import { ConnectorFileType } from './connector-file-type';
export interface Connector {
    filetypes?: Array<ConnectorFileType>;
    hasViewMode?: boolean;
    icon?: string;
    id?: string;
    onlyDesktop?: boolean;
    parameters?: Array<string>;

    /**
     * false
     */
    showNew: boolean;
}
