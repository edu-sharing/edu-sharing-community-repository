/* tslint:disable */
/* eslint-disable */
import { VersionProject } from './version-project';
export interface VersionMaven {
    bom?: {
        [key: string]: string;
    };
    project?: VersionProject;
}
