import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { SkipNavService, SkipTarget } from './skip-nav.service';

/**
 * Skip navigation for accessibility and keyboard users.
 *
 * The skip navigation is initially invisible. It is the first element to receive focus when the
 * user presses 'Tab' and becomes visible once it has focus. Skip links allow for quick navigation
 * around the page, skipping reoccurring elements such as navigation and banners.
 */
@Component({
    selector: 'es-skip-nav',
    templateUrl: './skip-nav.component.html',
    styleUrls: ['./skip-nav.component.scss'],
})
export class SkipNavComponent implements OnInit {
    availableTargets: Observable<SkipTarget[]>;

    constructor(private skipNav: SkipNavService) {}

    ngOnInit(): void {
        this.availableTargets = this.skipNav.getAvailableTargets();
    }

    skipTo(target: SkipTarget): void {
        this.skipNav.skipTo(target);
    }
}
