/* tslint:disable */
/* eslint-disable */
import { JobFieldDescription } from './job-field-description';
export interface JobDescription {
    description?: string;
    name?: string;
    params?: Array<JobFieldDescription>;
}
