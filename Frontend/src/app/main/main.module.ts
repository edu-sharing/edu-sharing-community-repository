import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { LoadingScreenComponent } from './loading-screen/loading-screen.component';
import { WorkspaceAddFolder } from './navigation/add-folder/add-folder.component';
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
import { TopBarComponent } from './navigation/top-bar/top-bar.component';
import { UserProfileComponent } from './navigation/user-profile/user-profile.component';

@NgModule({
    declarations: [
        LoadingScreenComponent,
        MainMenuBottomComponent,
        MainMenuButtonsComponent,
        MainMenuDropdownComponent,
        MainMenuSidebarComponent,
        MainNavComponent,
        TopBarComponent,
        SkipNavComponent,
        BannerComponent,
        SearchHeaderComponent,
        CreateMenuComponent,
        UserProfileComponent,
        SearchFieldComponent,
        WorkspaceAddFolder,
    ],
    imports: [SharedModule],
    exports: [MainNavComponent],
})
export class MainModule {}
