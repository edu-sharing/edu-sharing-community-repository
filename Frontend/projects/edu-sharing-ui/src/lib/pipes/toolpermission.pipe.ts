import { Pipe, PipeTransform } from '@angular/core';
import { AuthenticationService } from 'ngx-edu-sharing-api';

@Pipe({
    name: 'esToolpermission',
})
export class ToolpermissionPipe implements PipeTransform {
    constructor(private authenticationService: AuthenticationService) {}

    transform(toolpermission: string) {
        return this.authenticationService.hasToolpermission(toolpermission);
    }
}
