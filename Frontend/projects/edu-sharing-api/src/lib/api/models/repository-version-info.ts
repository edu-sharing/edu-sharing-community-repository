/* tslint:disable */
/* eslint-disable */
import { Version } from './version';
import { VersionGit } from './version-git';
import { VersionMaven } from './version-maven';
export interface RepositoryVersionInfo {
    git?: VersionGit;
    maven?: VersionMaven;
    version?: Version;
}
