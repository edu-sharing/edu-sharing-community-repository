import { Component, Input } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { EduSharingUiCommonModule } from 'ngx-edu-sharing-ui';
import { PointingOwlImageComponent } from './pointing-owl-image/pointing-owl-image.component';

@Component({
    selector: 'es-custom-propose-content-card',
    imports: [EduSharingUiCommonModule, MatButton, PointingOwlImageComponent, TranslateModule],
    templateUrl: './custom-propose-content-card.component.html',
    styleUrls: ['./custom-propose-content-card.component.scss'],
})
export class CustomProposeContentCardComponent {
    @Input() searchText: string;

    /**
     * Proposes content by opening a specific link.
     */
    proposeContent(): void {
        const url: string =
            'https://wirlernenonline.de/fachportalinhalte-vorschlagen/?type=material&headline=Fachportal&pageDiscipline=' +
            encodeURI(this.searchText);
        window.open(url, '_blank');
    }
}
