import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'es-loading-screen',
    templateUrl: './loading-screen.component.html',
    styleUrls: ['./loading-screen.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class LoadingScreenComponent {
    animationLoaded = false;
}
