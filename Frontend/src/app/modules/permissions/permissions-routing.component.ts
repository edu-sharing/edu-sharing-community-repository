import { Component } from '@angular/core';
import { MainNavService } from '../../main/navigation/main-nav.service';
@Component({
    selector: 'es-permissions',
    template: '<router-outlet></router-outlet>',
})
export class PermissionsRoutingComponent {
    mainNav = this.mainNavService.getMainNav();
    constructor(private mainNavService: MainNavService) {}
}
