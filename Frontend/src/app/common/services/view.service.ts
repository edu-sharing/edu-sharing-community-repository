import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

class ViewConfigClass {
    contrastMode = false;
}

export type ViewConfig = Partial<ViewConfigClass>;
export const VIEW_CONFIG = new InjectionToken<ViewConfig>('view.config');

@Injectable({
    providedIn: 'root',
})
export class ViewService {
    private readonly config: ViewConfigClass;
    private readonly contrastModeSubject: BehaviorSubject<boolean>;

    constructor(@Optional() @Inject(VIEW_CONFIG) config: ViewConfig) {
        this.config = { ...new ViewConfigClass(), ...(config ?? {}) };
        this.contrastModeSubject = new BehaviorSubject<boolean>(this.config.contrastMode);
    }

    setUp(): void {
        this.registerContrastModeSubject();
    }

    private registerContrastModeSubject(): void {
        const contrastModeClass = 'es-contrast-mode';
        this.contrastModeSubject.pipe(distinctUntilChanged()).subscribe((value) => {
            if (value) {
                document.body.classList.add(contrastModeClass);
            } else {
                document.body.classList.remove(contrastModeClass);
            }
        });
    }
}
