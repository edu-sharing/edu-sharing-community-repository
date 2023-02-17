import { Injectable, NgZone } from '@angular/core';
import { Params, Router } from '@angular/router';

@Injectable({
    providedIn: 'root',
})
export class NavigationScheduler {
    private readonly appendTimeout = 500;

    private timeout: ReturnType<typeof setTimeout> = null;
    /** Route for next navigation execution. */
    private route: string[] = [];
    /** Query params for next navigation execution. */
    private params: Params = null;
    /** `replaceUrl` value for next navigation execution. */
    private replaceUrl = true;
    /** While true, navigation actions will be appended to the previous navigation in history. */
    private shouldAppend = false;

    constructor(private router: Router, private ngZone: NgZone) {}

    /**
     * Schedules the given parameters for navigation.
     *
     * Subsequently scheduled navigation actions will combined into a single call to
     * `router.navigate`. After the execution of a navigation, there is a timeout (`appendTimeout`),
     * during which newly scheduled navigation actions will be appended to history using
     * `replaceUrl: true`.
     *
     * @param route will become the route to navigate to, replacing any route given in a previously
     * scheduled navigation
     * @param queryParams will be merged with other query params scheduled for navigation
     * @param replaceUrl will navigate with `replaceUrl: true` if all of the executed
     * navigation actions were scheduled with `replaceUrl: true` (default: `false`)
     */
    scheduleNavigation({
        route,
        queryParams,
        replaceUrl = false,
    }: {
        route?: string[];
        queryParams?: Params;
        replaceUrl?: boolean;
    }): void {
        if (route) {
            this.route = route;
        }
        this.params = {
            ...(this.params ?? {}),
            ...(queryParams ?? {}),
        };
        if (!replaceUrl) {
            this.replaceUrl = false;
        }
        // Schedule next navigation execution (if not already scheduled).
        if (!this.timeout) {
            this.timeout = setTimeout(() => {
                // Execute navigation.
                void this.router.navigate(this.route, {
                    queryParams: this.params,
                    queryParamsHandling: 'merge',
                    replaceUrl: this.shouldAppend || this.replaceUrl,
                });
                // Reset navigation parameters.
                this.timeout = null;
                this.route = [];
                this.params = null;
                this.replaceUrl = true;
                // Setup append phase (if not already appending).
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
