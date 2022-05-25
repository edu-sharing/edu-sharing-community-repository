/*
 * Public API Surface of ngx-edu-sharing-api
 */

export {
    EduSharingApiConfigurationParams,
    EDU_SHARING_API_CONFIG,
} from './lib/edu-sharing-api-configuration';
export * from './lib/edu-sharing-api.module';
export * from './lib/models';
export * from './lib/wrappers/about.service';
export * from './lib/wrappers/authentication.service';
export * from './lib/wrappers/config.service';
export * from './lib/wrappers/mds-label.service';
export * from './lib/wrappers/mds.service';
export * from './lib/wrappers/node.service';
export * from './lib/wrappers/nodeList.service';
export * from './lib/wrappers/search.service';
export * from './lib/wrappers/user.service';

// Unwrapped API services.
//
// Exporting services here indicates that these services are safe to use and don't interfere with
// any wrappers. If you make assumptions in wrappers about what API calls have been made, make sure
// to not expose these API calls here, but instead provide a wrapper that keeps track of calls.
export { MdsV1Service, StreamV1Service, ClientutilsV1Service } from './lib/api/services';