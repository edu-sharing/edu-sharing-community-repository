import { Injectable, Injector } from '@angular/core';
import { Toast } from './toast';

@Injectable({
    providedIn: 'root',
})
export class ErrorHandlerService {
    private toast: Toast;

    constructor(private injector: Injector) {
        // Prevent circular dependency.
        Promise.resolve().then(() => (this.toast = this.injector.get(Toast)));
    }

    handleError(error: any): void {
        this.toast.error(error);
    }
}
