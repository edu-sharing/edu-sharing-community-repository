import { Injectable, NgZone } from '@angular/core';
import { Params, Router } from '@angular/router';

@Injectable({
    providedIn: 'root',
})
export class NavigationScheduler {
    private route: string[] = [];
    private params: Params = null;
    private timeout: ReturnType<typeof setTimeout> = null;
    private shouldAppend = false;
    private readonly appendTimeout = 500;

    constructor(private router: Router, private ngZone: NgZone) {}

    scheduleNavigation({ queryParams, route }: { queryParams?: Params; route?: string[] }): void {
        // console.log('scheduleNavigation', queryParams);
        this.params = {
            ...(this.params ?? {}),
            ...(queryParams ?? {}),
        };
        if (route) {
            this.route = route;
        }
        if (!this.timeout) {
            this.timeout = setTimeout(() => {
                // console.log('navigate', { params: this.params, shouldAppend: this.shouldAppend });
                void this.router.navigate(this.route, {
                    queryParams: this.params,
                    queryParamsHandling: 'merge',
                    replaceUrl: this.shouldAppend,
                });
                this.timeout = null;
                this.route = [];
                this.params = null;
                if (!this.shouldAppend) {
                    this.shouldAppend = true;
                    this.ngZone.runOutsideAngular(() =>
                        setTimeout(() => (this.shouldAppend = false), this.appendTimeout),
                    );
                }
            });
        }
    }
}
