import { Component } from '@angular/core';

@Component({
    selector: 'es-loading-screen',
    templateUrl: './loading-screen.component.html',
    styleUrls: ['./loading-screen.component.scss'],
})
export class LoadingScreenComponent {
    animationLoaded = false;
}
