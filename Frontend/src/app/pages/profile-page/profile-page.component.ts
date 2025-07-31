import { firstValueFrom, forkJoin as observableForkJoin, of, Subject, timer } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
    ActionbarComponent,
    DefaultGroups,
    ElementType,
    NodePersonNamePipe,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    TranslationsService,
    UIAnimation,
    VCard,
} from 'ngx-edu-sharing-ui';
import {
    ConfigurationService,
    ProfileSettings,
    RestConnectorService,
    RestHelper,
    RestIamService,
    User,
    UserStats,
} from '../../core-module/core.module';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Toast, ToastType } from '../../services/toast';
import { trigger } from '@angular/animations';
import { Helper } from '../../core-module/rest/helper';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { catchError, take, takeUntil } from 'rxjs/operators';
import { ConfigService, HOME_REPOSITORY, IamV1Service, ME, Node } from 'ngx-edu-sharing-api';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { Closable } from '../../features/dialogs/card-dialog/card-dialog-config';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'es-profile-page',
    templateUrl: 'profile-page.component.html',
    styleUrls: ['profile-page.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
    providers: [OptionsHelperDataService],
    standalone: false,
})
export class ProfilePageComponent implements OnInit, OnDestroy {
    private destroyed = new Subject<void>();
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
    // dummy parameter to refetch the avatar
    avatarCache = '';
    gdprExport: Node;
    constructor(
        private toast: Toast,
        private route: ActivatedRoute,
        private dialogs: DialogsService,
        private mainNav: MainNavService,
        private connector: RestConnectorService,
        private translations: TranslationsService,
        private translate: TranslateService,
        private router: Router,
        private config: ConfigurationService,
        private configService: ConfigService,
        private sanitizer: DomSanitizer,
        private optionsHelperDataService: OptionsHelperDataService,
        private loadingScreen: LoadingScreenService,
        private iamServiceLegacy: RestIamService,
        private iamService: IamV1Service,
    ) {
        this.translations.waitForInit().subscribe(() => {
            route.params.subscribe((params) => {
                this.editProfileUrl = this.config.instant('editProfileUrl');
                this.editProfile = this.config.instant('editProfile', true);
                this.loadUser(params.authority);
                this.getProfileSetting(params.authority);
            });
        });
        this.editAction = new OptionItem('PROFILES.EDIT', 'edit', () => this.beginEdit());
        this.editAction.group = DefaultGroups.Edit;
        this.editAction.elementType = [ElementType.Unknown];
        this.editAction.showAsAction = true;
    }
    private static PASSWORD_MIN_LENGTH = 5;
    public user: User;
    public userStats: UserStats;
    public userEdit: User;
    public isMe: boolean;
    public edit: boolean;
    public avatarFile: any;
    public changePassword: boolean;
    public editAbout = false;
    public oldPassword = '';
    public password = '';
    gdprExportTriggered = false;
    // is editing allowed at all (via global config)
    editProfile: boolean;
    private editProfileUrl: string;
    avatarImage: any;
    profileSettings: ProfileSettings;
    @ViewChild('avatar') avatarElement: ElementRef;
    @ViewChild(ActionbarComponent) actionbarComponent: ActionbarComponent;
    // can the particular user profile (based on the source) be edited?
    userEditProfile: boolean;
    private editAction: OptionItem;
    showPersistentIds = false;

    ngOnInit(): void {
        this.mainNav.setMainNavConfig({
            title: 'PROFILES.TITLE_NAV',
            currentScope: 'profiles',
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    public loadUser(authority: string) {
        this.toast.showProgressSpinner();
        this.connector.isLoggedIn().subscribe((login) => {
            observableForkJoin(
                this.iamServiceLegacy.getUser(authority),
                this.iamServiceLegacy.getUserStats(authority),
            ).subscribe(
                ([profile, stats]) => {
                    this.user = profile.person;
                    this.userStats = stats;
                    this.userEditProfile = profile.editProfile;
                    this.toast.closeProgressSpinner();
                    this.userEdit = Helper.deepCopy(this.user);
                    if (!(this.user.profile.vcard instanceof VCard)) {
                        this.user.profile.vcard = new VCard(
                            this.user.profile.vcard as unknown as string,
                        );
                    }
                    this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
                    if (!this.loadingTask.isDone) {
                        this.loadingTask.done();
                    }
                    void this.iamServiceLegacy.getCurrentUserAsync().then((me) => {
                        this.isMe = profile.person.authorityName === me.person.authorityName;
                        if (this.isMe && login.isGuest) {
                            RestHelper.goToLogin(this.router, this.config);
                        }
                        if (this.isMe) {
                            this.iamService
                                .getDataProtectionExport({
                                    person: ME,
                                    repository: HOME_REPOSITORY,
                                })
                                .pipe(
                                    catchError((e) => {
                                        e.preventDefault();
                                        return of(null);
                                    }),
                                )
                                .subscribe((gdpr) => (this.gdprExport = gdpr?.node));
                        }

                        setTimeout(() => {
                            this.editAction.customEnabledCallback = async () =>
                                this.editProfile && !!(this.userEditProfile || this.editProfileUrl);
                            this.optionsHelperDataService.setData({
                                scope: Scope.UserProfile,
                                customOptions: {
                                    useDefaultOptions: false,
                                    addOptions: [this.editAction],
                                },
                            });
                            void this.optionsHelperDataService.initComponents(
                                this.actionbarComponent,
                            );
                            void this.optionsHelperDataService.refreshComponents();
                        });
                    });
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    if (!this.loadingTask.isDone) {
                        this.loadingTask.done();
                    }
                    this.toast.error(null, 'PROFILES.LOAD_ERROR');
                },
            );
        });
    }
    private getProfileSetting(authority: string) {
        this.iamServiceLegacy.getProfileSettings(authority).subscribe(
            (res: ProfileSettings) => {
                this.profileSettings = res;
            },
            (error: any) => {
                this.profileSettings = null;
            },
        );
    }
    public updateAvatar(event: any) {
        if (
            this.avatarElement.nativeElement.files &&
            this.avatarElement.nativeElement.files.length
        ) {
            this.avatarFile = this.avatarElement.nativeElement.files[0];
            this.avatarImage = this.sanitizer.bypassSecurityTrustUrl(
                URL.createObjectURL(this.avatarFile),
            );
        }
    }
    public beginEdit() {
        if (!this.userEditProfile && this.editProfileUrl) {
            window.location.href = this.editProfileUrl;
            return;
        }
        this.userEdit = Helper.deepCopy(this.user);
        this.userEdit.profile.vcard = this.user.profile.vcard.copy();
        this.edit = true;
        this.avatarFile = null;
    }
    public clearAvatar() {
        this.avatarFile = null;
        this.userEdit.profile.avatar = null;
    }
    public hasAvatar() {
        return this.userEdit.profile.avatar || this.avatarFile;
    }
    public savePassword() {
        if (this.changePassword) {
            this.toast.showProgressSpinner();
            if (this.password.length < ProfilePageComponent.PASSWORD_MIN_LENGTH) {
                this.toast.error(null, 'PASSWORD_MIN_LENGTH', {
                    length: ProfilePageComponent.PASSWORD_MIN_LENGTH,
                });
                this.toast.closeProgressSpinner();
                return;
            }
            const credentials = { oldPassword: this.oldPassword, newPassword: this.password };
            this.iamServiceLegacy
                .editUserCredentials(this.user.authorityName, credentials)
                .subscribe(
                    () => {
                        this.saveAvatar();
                    },
                    (error: any) => {
                        if (RestHelper.errorMessageContains(error, 'BadCredentialsException')) {
                            this.toast.error(null, 'WRONG_PASSWORD');
                            this.toast.closeProgressSpinner();
                        } else {
                            this.toast.error(error);
                            this.saveAvatar();
                        }
                    },
                );
        } else {
            this.saveAvatar();
        }
    }
    public saveEdits(validate = true) {
        if (validate) {
            if (!this.userEdit.profile.firstName?.trim()) {
                this.toast.error(null, 'PROFILES.ERROR.FIRST_NAME');
                return;
            }
            if (!this.userEdit.profile.lastName?.trim()) {
                this.toast.error(null, 'PROFILES.ERROR.LAST_NAME');
                return;
            }
            if (!this.userEdit.profile.email?.trim()) {
                this.toast.error(null, 'PROFILES.ERROR.EMAIL');
                return;
            }
        }
        this.toast.showProgressSpinner();
        this.iamServiceLegacy.editUser(this.user.authorityName, this.userEdit.profile).subscribe(
            () => {
                this.saveProfileSettings();
            },
            (error: any) => {
                this.toast.closeProgressSpinner();
                this.toast.error(error);
            },
        );
    }

    private saveAvatar() {
        this.user = null;
        if (!this.userEdit.profile.avatar && !this.avatarFile) {
            this.iamServiceLegacy.removeUserAvatar(this.userEdit.authorityName).subscribe(
                () => {
                    this.edit = false;
                    this.editAbout = false;
                    this.oldPassword = '';
                    this.password = '';
                    this.changePassword = false;
                    this.toast.toast('PROFILE_UPDATED');
                    this.loadUser(this.userEdit.authorityName);
                },
                (error) => {
                    this.toast.error(error);
                },
            );
        } else if (this.avatarFile) {
            this.iamServiceLegacy
                .setUserAvatar(this.avatarFile, this.userEdit.authorityName)
                .subscribe(
                    () => {
                        this.edit = false;
                        this.editAbout = false;
                        this.toast.toast('PROFILE_UPDATED');
                        this.loadUser(this.userEdit.authorityName);
                        // the backend is running an async refresh task
                        // so we wait here and try to refetch the icon
                        timer(0, 1000)
                            .pipe(take(10), takeUntil(this.destroyed))
                            .subscribe(() => {
                                this.avatarCache = '&dontcache = ' + Math.random();
                            });
                    },
                    (error) => {
                        this.toast.error(error);
                    },
                );
        } else {
            this.toast.closeProgressSpinner();
            this.edit = false;
            this.editAbout = false;
            this.toast.toast('PROFILE_UPDATED');
            this.loadUser(this.userEdit.authorityName);
        }
    }

    private saveProfileSettings() {
        this.iamServiceLegacy
            .setProfileSettings(this.profileSettings, this.user.authorityName)
            .subscribe(
                () => {
                    this.saveAvatar();
                },
                (error) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }
    public aboutEdit() {
        this.userEdit = Helper.deepCopy(this.user);
        this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
        this.editAbout = true;
    }

    public editPassword() {
        this.changePassword = !this.changePassword;
        this.password = '';
        this.oldPassword = '';
    }

    async requestGdprExport() {
        let message = await firstValueFrom(this.translate.get('PROFILES.GDPR.MESSAGE'));
        if (this.gdprExport) {
            message +=
                '\n\n' +
                (await firstValueFrom(this.translate.get('PROFILES.GDPR.MESSAGE_OVERWRITE')));
        }

        void this.dialogs.openGenericDialog({
            title: 'PROFILES.GDPR.TITLE',
            subtitle: new NodePersonNamePipe(this.configService).transform(this.user),
            message,
            avatar: {
                kind: 'icon',
                icon: 'archive',
            },
            closable: Closable.Casual,
            buttons: [
                {
                    label: 'CANCEL',
                    config: { color: 'standard' },
                    callback: async () => true,
                },
                {
                    label: 'PROFILES.GDPR.CONTINUE',
                    config: { color: 'primary' },
                    callback: async (ref) => {
                        ref.patchState({ isLoading: true });
                        await firstValueFrom(
                            this.iamService.requestDataProtectionExport({
                                person: ME,
                                repository: HOME_REPOSITORY,
                            }),
                        );
                        this.gdprExportTriggered = true;
                        this.toast.show({
                            message: 'PROFILES.GDPR.REQUEST_STARTED',
                            type: 'info',
                            subtype: ToastType.InfoData,
                        });
                        return true;
                    },
                },
            ],
        });
    }
    savePersistentIds() {
        this.saveEdits(false);
    }
}
