import { Injectable, Pipe, PipeTransform, inject } from '@angular/core';
import { AuthenticationService } from 'ngx-edu-sharing-api';

@Injectable({ providedIn: 'root' })
@Pipe({
    name: 'esToolpermission',
})
export class ToolpermissionPipe implements PipeTransform {
    private authenticationService = inject(AuthenticationService);

    transform(toolpermission: string) {
        return this.authenticationService.hasToolpermission(toolpermission);
    }
}
