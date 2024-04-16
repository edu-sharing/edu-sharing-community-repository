import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Toast } from '../../services/toast';

@Injectable({ providedIn: 'root' })
export class ErrorProcessingService {
    constructor(private toast: Toast) {}

    /**
     * handle a rest request including automatically showing and cancelling progress dialogs
     * and showing mapped error messages if the api failed
     */
    handleRestRequest<T>(request: Observable<T>, showProgress = true): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            if (showProgress) {
                this.toast.showProgressSpinner();
            }
            request.subscribe(
                (result) => {
                    if (showProgress) {
                        this.toast.closeProgressSpinner();
                    }
                    resolve(result);
                },
                (error) => {
                    if (showProgress) {
                        this.toast.closeProgressSpinner();
                    }
                    this.toast.error(error);
                    reject(error);
                },
            );
        });
    }
}
