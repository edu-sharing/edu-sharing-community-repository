import { Component, inject } from '@angular/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslateModule } from '@ngx-translate/core';
import { ScopesService } from 'ngx-edu-sharing-api';

/**
 * Lets an editor search the active scope like its readers (only published content): the search page
 * sends `SearchParameters.scopeRole`. Only a view of the search, the role itself (editorial page,
 * editing) is unchanged. Only offered to editors.
 */
@Component({
    selector: 'es-scope-view-as-reader',
    templateUrl: './scope-view-as-reader.component.html',
    styleUrls: ['./scope-view-as-reader.component.scss'],
    imports: [MatSlideToggleModule, TranslateModule],
})
export class ScopeViewAsReaderComponent {
    private scopesService = inject(ScopesService);

    protected readonly viewAsReader = this.scopesService.viewAsReader;
    protected readonly visible = this.scopesService.isEditor;

    toggle(enabled: boolean) {
        this.scopesService.setViewAsReader(enabled);
    }
}
