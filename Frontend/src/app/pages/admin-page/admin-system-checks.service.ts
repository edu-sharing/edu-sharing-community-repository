import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AboutService, JobInfo, NetworkService } from 'ngx-edu-sharing-api';
import { RenderHelperService } from 'ngx-edu-sharing-ui';
import { firstValueFrom } from 'rxjs';
import {
    ConfigurationService,
    RestAdminService,
    RestConstants,
    RestNodeService,
} from '../../core-module/core.module';
import { UIHelper } from '../../core-ui-module/ui-helper';

export type SystemCheckStatus = 'OK' | 'INFO' | 'WARN' | 'FAIL';
export type SystemCheck = {
    name: string;
    status: SystemCheckStatus;
    /** additional parameters passed to the i18n keys of this check */
    translate?: any;
    error?: any;
    /** invoked by the "solution" link of the check */
    callback?: () => void;
};

/**
 * Actions a check may offer as its solution link that are owned by the hosting admin page.
 */
export type SystemCheckActions = {
    /** switch the admin page to the applications section */
    goToApplications: () => void;
    /** switch to the applications section and open the home application xml for editing */
    editHomeApplication: () => void;
};

/**
 * Runs the checks of the admin page's "info" section (system checks + toolpermission checks).
 *
 * Deliberately not `providedIn: 'root'` - it is provided by the admin page only.
 */
@Injectable()
export class AdminSystemChecksService {
    /** toolpermissions that must not be granted to everyone */
    private static readonly CRITICAL_TOOLPERMISSIONS = [
        RestConstants.TOOLPERMISSION_USAGE_STATISTIC,
        RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
        RestConstants.TOOLPERMISSION_PUBLISH_COPY,
        RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_USER,
        RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_NODES,
    ];
    private static readonly STATUS_ORDER: Record<SystemCheckStatus, number> = {
        FAIL: 0,
        WARN: 1,
        INFO: 2,
        OK: 3,
    };

    private about = inject(AboutService);
    private admin = inject(RestAdminService);
    private config = inject(ConfigurationService);
    private networkService = inject(NetworkService);
    private node = inject(RestNodeService);
    private renderHelper = inject(RenderHelperService);
    private router = inject(Router);

    private readonly systemChecksSource = signal<SystemCheck[]>([]);
    private readonly toolpermissionChecksSource = signal<SystemCheck[]>([]);
    /** system checks, most severe first */
    readonly systemChecks = computed(() => this.sort(this.systemChecksSource()));
    /** toolpermission checks, most severe first */
    readonly toolpermissionChecks = computed(() => this.sort(this.toolpermissionChecksSource()));

    private actions: SystemCheckActions = {
        goToApplications: () => {},
        editHomeApplication: () => {},
    };

    /**
     * Registers the page-level actions the checks link to. Called once by the hosting page.
     */
    setActions(actions: SystemCheckActions): void {
        this.actions = actions;
    }

    runToolpermissionChecks(): void {
        this.toolpermissionChecksSource.set([]);
        this.admin.getToolpermissions(RestConstants.AUTHORITY_EVERYONE).subscribe((tp) => {
            this.toolpermissionChecksSource.set(
                AdminSystemChecksService.CRITICAL_TOOLPERMISSIONS.map((name) => ({
                    name,
                    status: tp[name].explicit === 'ALLOWED' ? 'FAIL' : 'OK',
                })),
            );
        });
    }

    /**
     * Revokes the explicit grant of a toolpermission reported by a failed check.
     */
    fixToolpermission(check: SystemCheck): void {
        this.toolpermissionChecksSource.set([]);
        this.admin.getToolpermissions(RestConstants.AUTHORITY_EVERYONE).subscribe((tpIn) => {
            const tp: any = {};
            Object.keys(tpIn).forEach((k) => (tp[k] = tpIn[k].explicit));
            tp[check.name] = 'UNDEFINED';
            this.admin
                .setToolpermissions(RestConstants.AUTHORITY_EVERYONE, tp)
                .subscribe(() => this.runToolpermissionChecks());
        });
    }

    runSystemChecks(): void {
        this.systemChecksSource.set([]);
        this.checkRenderingServiceVersion();
        this.checkRenderingService2();
        this.checkAppId();
        this.checkCompanyHomePermissions();
        this.checkRunningJobs();
        this.checkMailSetup();
        this.checkHomeApplication();
    }

    /** version check of the rendering service, only relevant for rendering service 1 */
    private checkRenderingServiceVersion(): void {
        void this.renderHelper.hasRenderingService2().then((hasRs2) => {
            if (hasRs2) {
                return;
            }
            this.about.getAbout().subscribe({
                next: (about) => {
                    const repositoryVersion = this.getMajorVersion(about.version.repository);
                    const renderServiceVersion = this.getMajorVersion(about.version.renderservice);
                    this.push({
                        name: 'RENDERING',
                        status:
                            repositoryVersion == 'unknown'
                                ? 'WARN'
                                : repositoryVersion == renderServiceVersion
                                ? 'OK'
                                : 'FAIL',
                        translate: about.version,
                        callback: () => this.actions.goToApplications(),
                    });
                },
                error: (error) => {
                    this.push({
                        name: 'RENDERING',
                        status: 'FAIL',
                        error,
                        callback: () => this.actions.goToApplications(),
                    });
                },
            });
        });
    }

    /**
     * Health check of rendering service 2: fetches its modules. Only added if rs2 is available.
     */
    private checkRenderingService2(): void {
        void (async () => {
            if (!(await this.renderHelper.hasRenderingService2())) {
                return;
            }
            const url = await this.renderHelper.getRenderingService2Url();
            try {
                const modules = await this.renderHelper.getRenderingService2Modules(
                    await this.getHomeRepositoryId(),
                );
                this.push({
                    name: 'RS2',
                    status: 'OK',
                    translate: { url, modules: modules.length },
                });
            } catch (error) {
                console.warn(error);
                this.push({
                    name: 'RS2',
                    status: 'FAIL',
                    error,
                    translate: { url, message: error?.message },
                });
            }
        })();
    }

    /** the app id of the home repository should have been changed from the default */
    private checkAppId(): void {
        void this.getHomeRepositoryId().then((id) => {
            this.push({
                name: 'APPID',
                status: id == 'local' ? 'WARN' : 'OK',
                translate: { id },
                callback: () => this.actions.editHomeApplication(),
            });
        });
    }

    private checkCompanyHomePermissions(): void {
        this.node.getNodePermissions(RestConstants.USERHOME).subscribe({
            next: (data) => {
                const everyone = data.permissions.localPermissions.permissions.some(
                    (perm) => perm.authority.authorityName == RestConstants.AUTHORITY_EVERYONE,
                );
                this.push(this.createCompanyHomeCheck(everyone ? 'FAIL' : 'OK'));
            },
            error: (error) => {
                this.push(this.createCompanyHomeCheck('FAIL', error));
            },
        });
    }

    private createCompanyHomeCheck(status: SystemCheckStatus, error: any = null): SystemCheck {
        return {
            name: 'COMPANY_HOME',
            status,
            error,
            callback: () => {
                this.node.getNodeMetadata(RestConstants.USERHOME).subscribe((node) => {
                    UIHelper.goToWorkspaceFolder(this.router, null, node.node.parent.id);
                });
            },
        };
    }

    private checkRunningJobs(): void {
        this.admin.getJobs().subscribe((jobs) => {
            const count = jobs.filter((job: JobInfo) => job.status == 'Running').length;
            this.push({
                name: 'JOBS_RUNNING',
                status: count == 0 ? 'OK' : 'WARN',
                translate: { count },
            });
        });
    }

    /** status of nodeReport + mail server */
    private checkMailSetup(): void {
        this.admin.getConfigMerged().subscribe((config) => {
            const mail = config.repository.mail;
            if (this.config.instant('nodeReport', false)) {
                this.push({
                    name: 'MAIL_REPORT',
                    status: mail.report.receivers && mail.server.smtp.host ? 'OK' : 'FAIL',
                    translate: {
                        receivers: mail.report?.receivers?.join(', '),
                    },
                });
            }
            this.push({
                name: 'MAIL_SETUP',
                status: mail.server.smtp.host ? 'OK' : 'FAIL',
                translate: mail.server.smtp,
            });
        });
    }

    private checkHomeApplication(): void {
        this.admin.getApplicationXML(RestConstants.HOME_APPLICATION_XML).subscribe((home) => {
            this.push({
                name: 'CORS',
                status: home.allow_origin
                    ? home.allow_origin.indexOf('http://localhost:54361') != -1
                        ? 'OK'
                        : 'INFO'
                    : 'FAIL',
                translate: home,
                callback: () => this.actions.editHomeApplication(),
            });
            void this.checkRenderingServiceXss(home);
        });
    }

    /**
     * Checks that the rendering service is not hosted on the repository's domain, since scripts
     * delivered by it would otherwise have access to the repository api (xss). The url to check is
     * the one of the active rendering service. Independent of the rs2 health check.
     */
    private async checkRenderingServiceXss(home: any): Promise<void> {
        const domainRepo = home.domain;
        const renderUrl = (await this.renderHelper.hasRenderingService2())
            ? await this.renderHelper.getRenderingService2Url()
            : home.contenturl;
        let domainRender: string;
        try {
            domainRender = new URL(renderUrl).host;
        } catch (e) {
            console.warn(e);
        }
        this.push({
            name: 'RS_XSS',
            status: domainRepo == domainRender ? 'FAIL' : home.allow_origin ? 'OK' : 'INFO',
            translate: { repo: domainRepo, render: domainRender },
        });
    }

    private async getHomeRepositoryId(): Promise<string> {
        const repositories = await firstValueFrom(this.networkService.getRepositories());
        return repositories.find((repo) => repo.isHomeRepo)?.id;
    }

    private getMajorVersion(version: string): string {
        const v = version.split('.');
        if (v.length < 3) {
            return version;
        }
        v.splice(2, v.length - 2);
        return v.join('.');
    }

    private push(check: SystemCheck): void {
        this.systemChecksSource.update((checks) => checks.concat(check));
    }

    private sort(checks: SystemCheck[]): SystemCheck[] {
        const order = AdminSystemChecksService.STATUS_ORDER;
        return [...checks].sort((a, b) =>
            order[a.status] != order[b.status]
                ? order[a.status] - order[b.status]
                : a.name.localeCompare(b.name),
        );
    }
}
