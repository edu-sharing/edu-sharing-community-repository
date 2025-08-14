import { Component, OnDestroy } from '@angular/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { LoadingScreenComponent } from '../../main/loading-screen/loading-screen.component';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { Subject } from 'rxjs';

@Component({
    selector: 'es-loading-page',
    templateUrl: 'loading-page.component.html',
    styleUrls: ['loading-page.component.scss'],
    imports: [],
    standalone: true,
})
export class LoadingPageComponent implements OnDestroy {
    private destroyed$ = new Subject<void>();

    constructor(private loadingScreenService: LoadingScreenService) {
        this.loadingScreenService.addLoadingTask({
            startup: true,
            until: this.destroyed$,
        });
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
