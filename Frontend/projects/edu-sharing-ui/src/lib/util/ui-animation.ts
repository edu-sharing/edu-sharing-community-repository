import { style, animate, transition, trigger, state } from '@angular/animations';
export class UIAnimation {
    public static ANIMATION_TIME_FAST = 100;
    public static ANIMATION_TIME_NORMAL = 200;
    public static ANIMATION_TIME_SLOW = 300;
    public static fade(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [style({ opacity: 0 }), animate(time, style({ opacity: 1 }))]),
            transition(':leave', [animate(time, style({ opacity: 0 }))]),
        ];
    }
    public static fromBottom(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ transform: 'translateY(100%)' }),
                animate(time, style({ transform: 'translateY(0)' })),
            ]),
            transition(':leave', [
                style({ transform: 'translateY(0)' }),
                animate(time, style({ transform: 'translateY(100%)' })),
            ]),
        ];
    }
    public static fromLeft(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ transform: 'translateX(-100%)' }),
                animate(time, style({ transform: 'translateX(0)' })),
            ]),
            transition(':leave', [
                style({ transform: 'translateX(0)' }),
                animate(time, style({ transform: 'translateX(-100%)' })),
            ]),
        ];
    }
    public static fromRight(time: number | string = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ transform: 'translateX(100%)' }),
                animate(time, style({ transform: 'translateX(0)' })),
            ]),
            transition(':leave', [
                style({ transform: 'translateX(0)' }),
                animate(time, style({ transform: 'translateX(100%)' })),
            ]),
        ];
    }
    public static infobarBottom(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ bottom: '-70px' }),
                animate(time + 'ms ease', style({ bottom: '0' })),
            ]),
            transition(':leave', [
                style({ bottom: '0' }),
                animate(time + 'ms ease', style({ bottom: '-70px' })),
            ]),
        ];
    }
    /**
     * Useful animation for opening any overflow menus
     * @param time
     * @returns {AnimationStateTransitionMetadata[]}
     */
    public static openOverlay(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ 'transform-origin': '50% 0%', transform: 'scaleY(0.5)', opacity: 0 }),
                animate(time, style({ transform: 'scaleY(1)', opacity: 1 })),
            ]),
            transition(':leave', [
                style({ 'transform-origin': '50% 0%', transform: 'scaleY(1)', opacity: 1 }),
                animate(time, style({ transform: 'scaleY(0.5)', opacity: 0 })),
            ]),
        ];
    }
    /**
     * Useful animation for opening any overflow menus - inverted (from bottom to top)
     * @param time
     * @returns {AnimationStateTransitionMetadata[]}
     */
    public static openOverlayBottom(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ 'transform-origin': '50% 100%', transform: 'scaleY(0.5)', opacity: 0 }),
                animate(time, style({ transform: 'scaleY(1)', opacity: 1 })),
            ]),
            transition(':leave', [
                style({ 'transform-origin': '50% 100%', transform: 'scaleY(1)', opacity: 1 }),
                animate(time, style({ transform: 'scaleY(0.5)', opacity: 0 })),
            ]),
        ];
    }
    /**
     * Useful animation to switch different content insides a dialog (navigating between contents)
     * @param time
     * @returns {AnimationStateTransitionMetadata[]}
     */
    public static switchDialog(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({ 'transform-origin': '50% 0%', transform: 'scaleY(0)', opacity: 0 }),
                animate(time, style({ transform: 'scaleY(1)', opacity: 1 })),
            ]),
        ];
    }
    /**
     * Useful animation to switch different content insides a dialog (navigating between contents)
     * This is a special variant which does work without ngIf fields, it will remove display states
     */
    public static switchDialogBoolean(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            state(
                'false',
                style({
                    opacity: 0,
                    'transform-origin': '50% 0%',
                    transform: 'scaleY(0)',
                    display: 'none',
                }),
            ),
            state(
                'true',
                style({ opacity: 1, 'transform-origin': '50% 0%', transform: 'scaleY(100%)' }),
            ),
            transition('false => true', [style({ display: '' }), animate(time + 'ms ease-in-out')]),
            transition('true => false', [animate(time + 'ms ease-in-out')]),
        ];
    }
    /**
     * Useful animation for showing any modal cards
     * @param time
     * @returns {AnimationStateTransitionMetadata[]}
     */
    public static cardAnimation(time = UIAnimation.ANIMATION_TIME_NORMAL) {
        return [
            transition(':enter', [
                style({
                    'transform-origin': '50% 0%',
                    transform: 'scaleY(0.5) translateY(-100%)',
                    opacity: 0,
                }),
                animate(
                    time + 'ms ease',
                    style({ transform: 'scaleY(1) translateY(0)', opacity: 1 }),
                ),
            ]),
            transition(':leave', [
                style({ 'transform-origin': '50% 0%', opacity: 1 }),
                animate(time + 'ms ease', style({ opacity: 0 })),
            ]),
        ];
    }
}
