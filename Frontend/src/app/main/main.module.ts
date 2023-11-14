import { NgModule } from '@angular/core';
import { extensionDeclarationsMap } from '../extension/extension-declarations-map';
import { MdsModule } from '../features/mds/mds.module';
import { SharedModule } from '../shared/shared.module';
import { CookieInfoComponent } from './cookie-info/cookie-info.component';
import { LoadingScreenComponent } from './loading-screen/loading-screen.component';
import { BannerComponent } from './navigation/banner/banner.component';
import { CreateMenuComponent } from './navigation/create-menu/create-menu.component';
import { SearchHeaderComponent } from './navigation/header/header.component';
import { MainMenuBottomComponent } from './navigation/main-menu-bottom/main-menu-bottom.component';
import { MainMenuButtonsComponent } from './navigation/main-menu-buttons/main-menu-buttons.component';
import { MainMenuDropdownComponent } from './navigation/main-menu-dropdown/main-menu-dropdown.component';
import { MainMenuSidebarComponent } from './navigation/main-menu-sidebar/main-menu-sidebar.component';
import { MainNavComponent } from './navigation/main-nav/main-nav.component';
import { SearchFieldComponent } from './navigation/search-field/search-field.component';
import { SkipNavComponent } from './navigation/skip-nav/skip-nav.component';
import { NotificationDialogModule } from './navigation/top-bar/notification-dialog/notification-dialog.module';
import { TopBarComponent } from './navigation/top-bar/top-bar.component';
import { UserProfileComponent } from './navigation/user-profile/user-profile.component';
import { RocketchatComponent } from './rocketchat/rocketchat.component';
import { ScrollToTopButtonComponent } from './scroll-to-top-button/scroll-to-top-button.component';

@NgModule({
    declarations: [
        BannerComponent,
        CookieInfoComponent,
        CreateMenuComponent,
        LoadingScreenComponent,
        MainMenuBottomComponent,
        MainMenuButtonsComponent,
        MainMenuDropdownComponent,
        MainMenuSidebarComponent,
        MainNavComponent,
        RocketchatComponent,
        ScrollToTopButtonComponent,
        SearchFieldComponent,
        SearchHeaderComponent,
        SkipNavComponent,
        TopBarComponent,
        UserProfileComponent,
        extensionDeclarationsMap['MainModule'] || [],
    ],
    imports: [SharedModule, MdsModule, NotificationDialogModule],
    exports: [
        CookieInfoComponent,
        CreateMenuComponent,
        MainNavComponent,
        RocketchatComponent,
        ScrollToTopButtonComponent,
        SearchFieldComponent,
    ],
})
export class MainModule {}
