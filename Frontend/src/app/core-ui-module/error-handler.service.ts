import {Toast} from './toast';
import {Err} from 'typedoc/dist/lib/utils/result';
import {Injectable} from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class ErrorHandlerService {
    private static toast: Toast;
    constructor(
        private toast: Toast,
    ) {
        ErrorHandlerService.toast = toast;
    }
    static handleError(error: any) {
        ErrorHandlerService.toast.error(error);
    }
}
