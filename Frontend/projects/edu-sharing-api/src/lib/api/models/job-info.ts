/* tslint:disable */
/* eslint-disable */
import { JobDetail } from './job-detail';
import { Level } from './level';
import { LogEntry } from './log-entry';
export interface JobInfo {
    finishTime?: number;
    jobDetail?: JobDetail;
    log?: Array<LogEntry>;
    startTime?: number;
    status?: 'Running' | 'Failed' | 'Aborted' | 'Finished';
    worstLevel?: Level;
}
