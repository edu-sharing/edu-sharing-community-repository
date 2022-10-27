import { Browser } from '@playwright/test';
import { adminLogin, defaultLogin } from './constants';
import { getStorageStatePath } from './util';

type ToolPermissionState = 'ALLOWED' | 'DENIED' | 'UNDEFINED';
type ToolPermissionKey = 'TOOLPERMISSION_CONFIDENTAL';
type ToolPermissions = { [key in ToolPermissionKey]: ToolPermissionState };

export class EduSharingApi {
    constructor(private browser: Browser) {}

    /**
     * Resets all explicit tool permissions for the given user to the given values.
     */
    async resetToolPermissions(toolPermissions: ToolPermissions, username = defaultLogin.username) {
        const adminContext = await this.browser.newContext({
            storageState: getStorageStatePath(adminLogin),
        });
        await adminContext.request.put(`./rest/admin/v1/toolpermissions/${username}`, {
            headers: {
                Accept: 'application/json',
            },
            data: toolPermissions,
            failOnStatusCode: true,
        });
    }
}
