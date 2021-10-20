/* tslint:disable */
/* eslint-disable */
import { Pagination } from './pagination';
import { UserSimple } from './user-simple';
export interface UserEntries {
    pagination: Pagination;
    users: Array<UserSimple>;
}
