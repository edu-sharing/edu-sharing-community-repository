import { trigger } from '@angular/animations';
import { Component, HostListener } from '@angular/core';
import { UIAnimation } from 'src/app/core-module/ui/ui-animation';

/**
 * Shows a small button on the bottom of the screen that scrolls to the page top.
 * 
 * Place after all visible content for correct keyboard focus order.
 */
@Component({
    selector: 'es-scroll-to-top-button',
    templateUrl: './scroll-to-top-button.component.html',
    styleUrls: ['./scroll-to-top-button.component.scss'],
    animations: [trigger('fade', UIAnimation.fade())],
})
export class ScrollToTopButtonComponent {
    showScrollToTop = false;

    constructor() {}

    @HostListener('window:scroll')
    handleScroll() {
        this.showScrollToTop = (window.pageYOffset || document.documentElement.scrollTop) > 400;
    }

    scrollToTop() {
        window.scroll({
            top: 0,
            behavior: 'smooth',
        });
    }
}
