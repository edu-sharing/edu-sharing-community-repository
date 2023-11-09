import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ApplyToLmsComponent } from './common/ui/apply-to-lms/apply-to-lms.component';
import { UIConstants } from './core-module/core.module';
import { extensionRoutes } from './extension/extension-routes';
import { DialogsNavigationGuard } from './features/dialogs/dialogs-navigation.guard';
import { LtiComponent } from './modules/lti/lti.component';
import { OerComponent } from './modules/oer/oer.component';
import { ServicesComponent } from './modules/services/services.components';
import { StartupComponent } from './modules/startup/startup.component';
import { StreamComponent } from './modules/stream/stream.component';

const routes: Routes = [
    // overrides and additional routes
    ...extensionRoutes,

    // global
    { path: '', component: StartupComponent },

    {
        path: UIConstants.ROUTER_PREFIX + 'app',
        loadChildren: () =>
            import('./pages/app-login-page/app-login-page.module').then(
                (m) => m.AppLoginPageModule,
            ),
    },
    {
        path: UIConstants.ROUTER_PREFIX + 'app/share',
        loadChildren: () =>
            import('./pages/app-share-page/app-share-page.module').then(
                (m) => m.AppSharePageModule,
            ),
    },
    {
        path: UIConstants.ROUTER_PREFIX + 'render',
        loadChildren: () =>
            import('./pages/render-page/render-page.module').then((m) => m.RenderPageModule),
    },
    {
        path: UIConstants.ROUTER_PREFIX + 'apply-to-lms/:repo/:node',
        component: ApplyToLmsComponent,
    },
    // search
    {
        path: UIConstants.ROUTER_PREFIX + 'search',
        loadChildren: () =>
            import('./pages/search-page/search-page.module').then((m) => m.SearchPageModule),
    },
    // workspace
    {
        path: UIConstants.ROUTER_PREFIX + 'workspace',
        loadChildren: () =>
            import('./pages/workspace-page/workspace-page.module').then(
                (m) => m.WorkspacePageModule,
            ),
    },
    // collections
    {
        path: UIConstants.ROUTER_PREFIX + 'collections',
        loadChildren: () =>
            import('./pages/collections-page/collections-page.module').then(
                (m) => m.CollectionsPageModule,
            ),
    },
    // login
    {
        path: UIConstants.ROUTER_PREFIX + 'login',
        loadChildren: () =>
            import('./pages/login-page/login-page.module').then((m) => m.LoginPageModule),
    },
    // register
    {
        path: UIConstants.ROUTER_PREFIX + 'register',
        loadChildren: () =>
            import('./pages/register-page/register-page.module').then((m) => m.RegisterPageModule),
    },
    // file upload
    {
        path: UIConstants.ROUTER_PREFIX + 'upload',
        loadChildren: () =>
            import('./pages/upload-page/upload-page.module').then((m) => m.UploadPageModule),
    },
    // admin
    {
        path: UIConstants.ROUTER_PREFIX + 'admin',
        loadChildren: () =>
            import('./pages/admin-page/admin-page.module').then((m) => m.AdminPageModule),
    },
    // permissions
    {
        path: UIConstants.ROUTER_PREFIX + 'permissions',
        loadChildren: () =>
            import('./pages/user-management-page/user-management-page.module').then(
                (m) => m.UserManagementPageModule,
            ),
    },
    // oer
    { path: UIConstants.ROUTER_PREFIX + 'oer', component: OerComponent },
    // stream
    { path: UIConstants.ROUTER_PREFIX + 'stream', component: StreamComponent },
    // profiles
    {
        path: UIConstants.ROUTER_PREFIX + 'profiles',
        loadChildren: () =>
            import('./pages/profile-page/profile-page.module').then((m) => m.ProfilePageModule),
    },

    // link-share
    {
        path: UIConstants.ROUTER_PREFIX + 'sharing',
        loadChildren: () =>
            import('./pages/sharing-page/sharing-page.module').then((m) => m.SharingPageModule),
    },
    // services
    { path: UIConstants.ROUTER_PREFIX + 'services', component: ServicesComponent },

    // embed
    {
        path: UIConstants.ROUTER_PREFIX + 'embed/:component',
        loadChildren: () => import('./common/ui/embed/embed.module').then((m) => m.EmbedModule),
    },

    { path: UIConstants.ROUTER_PREFIX + 'lti', component: LtiComponent },

    // error page / 404
    {
        path: '',
        loadChildren: () =>
            import('./pages/error-page/error-page.module').then((m) => m.ErrorPageModule),
    },
];

@NgModule({
    imports: [
        RouterModule.forRoot(
            [
                // Add a `canDeactivate` guard to all routes, that closes any open dialogs before
                // allowing navigation.
                {
                    path: '',
                    canDeactivate: [DialogsNavigationGuard],
                    children: routes,
                    runGuardsAndResolvers: 'paramsOrQueryParamsChange',
                },
            ],
            {
                // scrollPositionRestoration: 'enabled' emulated via
                // ScrollPositionRestorationService. This prevents the browser history from getting
                // messed up when navigation attempts are cancelled by guards.
                canceledNavigationResolution: 'computed',
            },
        ),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule {}
