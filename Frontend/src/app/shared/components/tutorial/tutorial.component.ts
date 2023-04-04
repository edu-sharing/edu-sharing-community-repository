import { trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    Output,
    ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { UIAnimation } from 'ngx-edu-sharing-ui';

interface Dimensions {
    size: number;
    x: number;
    y: number;
}

@Component({
    selector: 'es-tutorial',
    templateUrl: 'tutorial.component.html',
    styleUrls: ['tutorial.component.scss'],
    animations: [trigger('fade', UIAnimation.fade())],
})
export class TutorialComponent {
    private static activeTutorial: ElementRef = null;
    private static PADDING_TOLERANCE = 50;

    @ViewChild('tutoral') tutorial: ElementRef;

    @Input() rgbColor = [0, 0, 0];
    @Input() showSkip = true;
    @Input() heading: string;
    @Input() description: string;
    @Input() set element(element: ElementRef) {
        this.setElement(element);
    }
    @Output() onNext = new EventEmitter();
    @Output() onSkip = new EventEmitter();

    background: SafeStyle;
    show = false;
    pos: {
        left?: string;
        top?: string;
        width?: string;
    } = {};

    private interval: any;

    constructor(private sanitizer: DomSanitizer) {}

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key === 'Escape' && this.show) {
            this.next();
            event.preventDefault();
            event.stopPropagation();
        }
    }

    wasTutorialShown(heading: string) {
        const item = localStorage.getItem('TUTORIAL.' + heading);
        return item;
    }

    setTutorialShown(heading: string) {
        localStorage.setItem('TUTORIAL.' + heading, 'true');
    }

    finish() {
        this.setTutorialShown(this.heading);
        clearInterval(this.interval);
        this.show = false;
        TutorialComponent.activeTutorial = null;
    }

    skip() {
        this.finish();
        this.onSkip.emit();
    }

    next() {
        this.finish();
        this.onNext.emit();
    }

    private setElement(element: ElementRef) {
        if (!element) {
            return;
        }
        if (!element.nativeElement) {
            console.warn(
                'Provided tutorial element does not have a native element. ' +
                    'Maybe you need to use esElementRef to get the right template variable. ' +
                    'See https://angular.io/guide/template-reference-variables#how-angular-assigns-values-to-template-variables.',
                element,
            );
            return;
        }
        if (TutorialComponent.activeTutorial) {
            setTimeout(() => this.setElement(element), 500);
            return;
        }
        TutorialComponent.activeTutorial = element;
        this.registerElement(element);
    }

    private registerElement(element: ElementRef): void {
        // FIXME: `setInterval` is quite heavy on Angular's change detection. Avoid if possible.
        this.interval = setInterval(() => {
            if (this.wasTutorialShown(this.heading)) {
                this.finish();
                return;
            }
            if (!element || !element.nativeElement) {
                return;
            }
            if (!this.show) {
                this.open();
            }
            const dimensions = this.getDimensions(element);
            this.setBackground(dimensions);
            setTimeout(() => {
                this.setPos(dimensions);
            });
        }, 1000 / 30);
    }

    private open(): void {
        this.show = true;
        setTimeout(() => {
            this.tutorial.nativeElement.focus();
        });
    }

    private getDimensions(element: ElementRef): Dimensions {
        const rect = element.nativeElement.getBoundingClientRect();
        const size =
            Math.min(
                Math.max(window.innerWidth, window.innerHeight) / 4,
                Math.max(rect.width, rect.height),
            ) / 2;
        const x = rect.left + rect.width / 2;
        const y = rect.top + rect.height / 2;
        return { size, x, y };
    }

    private setBackground({ size, x, y }: Dimensions): void {
        this.background = this.sanitizer.bypassSecurityTrustStyle(
            'radial-gradient(circle at ' +
                x +
                'px ' +
                y +
                'px, transparent 0,' +
                'transparent ' +
                size +
                'px,' +
                'rgba(' +
                this.rgbColor[0] +
                ',' +
                this.rgbColor[1] +
                ',' +
                this.rgbColor[2] +
                ',0.9) ' +
                (size + TutorialComponent.PADDING_TOLERANCE / 2) +
                'px,' +
                'rgba(' +
                this.rgbColor[0] +
                ',' +
                this.rgbColor[1] +
                ',' +
                this.rgbColor[2] +
                ',0.9) 70%)',
        );
    }

    private setPos({ size, x, y }: Dimensions): void {
        if (!this.tutorial || !this.tutorial.nativeElement) {
            return;
        }
        const pos = this.tutorial.nativeElement.getBoundingClientRect();
        this.pos = {};
        const space: number[] = [];
        space.push((window.innerWidth - x - size) * window.innerHeight);
        space.push((window.innerHeight - y - size) * window.innerWidth);
        space.push((x - size) * window.innerHeight);
        space.push((y - size) * window.innerWidth);

        let maxIndex = space.indexOf(Math.max(...space));
        // we prefer a centered region if we are on a big screen
        if (space[1] * 2 > space[0] && y < window.innerHeight / 3) maxIndex = 1;
        let diffX = 0;
        let diffY = 0;
        if (maxIndex === 0) {
            diffX = x + size;
        }
        if (maxIndex === 1) {
            diffY = y + size;
        }
        if (maxIndex === 2) {
            diffX = 1;
        }
        if (maxIndex === 3) {
            diffY = 1;
        }
        if (diffX > 0) {
            this.pos.left = Math.min(window.innerWidth - pos.width, diffX) + 'px';
            this.pos.top = Math.max(0, window.innerHeight / 2 - pos.height / 2) + 'px';
            this.pos.width = window.innerWidth - y - size + 'px';
        }
        if (diffY > 0) {
            this.pos.top = Math.min(window.innerHeight - pos.height, diffY) + 'px';
            this.pos.left = Math.max(0, window.innerWidth / 2 - pos.width / 2) + 'px';
        }
    }
}
