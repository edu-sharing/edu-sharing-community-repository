import {
    ComponentFactoryResolver,
    ComponentRef,
    EmbeddedViewRef,
    Injectable,
    Injector,
    NgZone,
    Type,
    ViewContainerRef,
    inject,
} from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { UIConstants } from '../util/ui-constants';
import { OptionItem, OptionItemToggle } from '../types/option-item';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class UIService {
    protected componentFactoryResolver = inject(ComponentFactoryResolver);
    protected injector = inject(Injector);
    protected ngZone = inject(NgZone);

    isTouchSubject = new BehaviorSubject(false);
    private metaKeyPressedSubject = new BehaviorSubject(false);
    private shiftKeyPressedSubject = new BehaviorSubject(false);
    private ctrlKeyPressedSubject = new BehaviorSubject(false);

    get shiftKeyPressed() {
        return this.shiftKeyPressedSubject.value;
    }

    constructor() {
        // HostListener not working, so use window
        this.ngZone.runOutsideAngular(() => {
            window.addEventListener('keydown', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('keyup', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                this.updateModifierKeys(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                const isTouch = (event as PointerEvent).pointerType === 'touch';
                if (this.isTouchSubject.value !== isTouch) {
                    this.ngZone.run(() => this.isTouchSubject.next(isTouch));
                }
            });
        });
    }
    private onKeyDownOrKeyUp(event: KeyboardEvent) {
        // `event.metaKey` is not consistent across browsers on the actual keypress of the modifier
        // key. So we handle these events separately.
        if (event.key === 'Control') {
            this.ctrlKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Shift') {
            this.shiftKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Meta') {
            this.metaKeyPressedSubject.next(event.type === 'keydown');
        } else {
            // In case we miss modifier events because the browser didn't have focus during the
            // event, we update modifier keys on unrelated key events as well.
            this.updateModifierKeys(event);
        }
    }

    private updateModifierKeys(event: PointerEvent | KeyboardEvent) {
        this.metaKeyPressedSubject.next(event.metaKey);
        this.shiftKeyPressedSubject.next(event.shiftKey);
        this.ctrlKeyPressedSubject.next(event.ctrlKey);
    }

    observeCtrlOrCmdKeyPressedOutsideZone(): Observable<boolean> {
        return rxjs.combineLatest([this.metaKeyPressedSubject, this.ctrlKeyPressedSubject]).pipe(
            map(([metaKeyPressed, ctrlKeyPressed]) => metaKeyPressed || ctrlKeyPressed),
            distinctUntilChanged(),
        );
    }

    /**
     * @Deprecated
     * Prefer to subscribe to the isTouchSubject directly if viable
     *
     * Returns true if the current sessions seems to be running on a mobile device
     * Please note that this means it's a touch device and does not tell anything about the screen size
     */
    public isMobile() {
        return this.isTouchSubject.value;
    }
    public static isMobileWidth() {
        return UIService.evaluateMediaQuery(
            UIConstants.MEDIA_QUERY_MAX_WIDTH,
            UIConstants.MOBILE_TAB_SWITCH_WIDTH,
        );
    }
    public static evaluateMediaQuery(type: string, value: number) {
        if (type == UIConstants.MEDIA_QUERY_MAX_WIDTH) return value > window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MIN_WIDTH) return value < window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MAX_HEIGHT) return value > window.innerHeight;
        if (type == UIConstants.MEDIA_QUERY_MIN_HEIGHT) return value < window.innerHeight;
        console.warn('Unsupported media query ' + type);
        return true;
    }
    filterValidOptions(options: OptionItem[]) {
        if (options == null) return null;
        options = options.filter((value) => value != null);
        let optionsFiltered: OptionItem[] = [];
        for (let option of options) {
            if (
                (!option.onlyMobile || (option.onlyMobile && this.isMobile())) &&
                (!option.onlyDesktop || (option.onlyDesktop && !this.isMobile())) &&
                (!option.mediaQueryType ||
                    (option.mediaQueryType &&
                        UIService.evaluateMediaQuery(
                            option.mediaQueryType,
                            option.mediaQueryValue,
                        )))
            )
                optionsFiltered.push(option);
        }
        return optionsFiltered;
    }

    /**
     * helper that updates the "isEnabled" flag on all options for the given, selected node
     * can be used by dropdown or action menus to update the state for the current element
     * @param options
     */
    async updateOptionEnabledState(options: BehaviorSubject<OptionItem[]>, objects: Node[] = null) {
        options.value?.forEach((o) => {
            o.isEnabled = !o.customEnabledCallback || !o.enabledCallback;
            if (o.enabledCallback) {
                void o.enabledCallback(objects).then((result) => {
                    o.isEnabled = result;
                    options.next(options.value);
                });
            }
        });
        options.next(options.value);
    }

    public filterToggleOptions(
        options: OptionItem[],
        toggle: boolean,
        togglePosition: 'before' | 'after' = 'after',
    ) {
        let result: OptionItem[] = [];
        for (let option of options) {
            if (
                ((option as OptionItemToggle).isToggle === toggle ||
                    (!toggle && !(option as OptionItemToggle).isToggle)) &&
                (!toggle || togglePosition === (option as OptionItemToggle).togglePosition)
            )
                result.push(option);
        }
        return result;
    }
    /**
     * dynamically inject an angular component into a regular html dom element
     * @param componentFactoryResolver The resolver service
     * @param viewContainerRef The viewContainerRef service
     * @param componentName The name of the angular component (e.g. SpinnerComponent)
     * @param targetElement The target element of the dom. If the element is null (not found), nothing is done
     * @param bindings Optional bindings (inputs & outputs) to the given component
     * @param delay Optional inflating delay in ms(some components may need some time to "init" the layout)
     * @param replace Whether to replace to previous `innerHTML` of `targetElement`
     * @param injector (to fetch templates for the component)
     */
    public injectAngularComponent<T>(
        viewContainerRef: ViewContainerRef,
        componentName: Type<T>,
        targetElement: Element,
        bindings: { [key: string]: any } = null,
        { delay = 0, replace = true } = {},
        injector?: Injector,
    ): ComponentRef<T> {
        if (targetElement == null) {
            return null;
        }
        const factory = this.componentFactoryResolver.resolveComponentFactory(componentName);
        const component: ComponentRef<T> = viewContainerRef.createComponent(
            factory,
            undefined,
            injector || this.injector,
        );
        if (bindings) {
            const instance: { [key: string]: any } = component.instance;
            for (const key in bindings) {
                const binding = bindings[key];
                if (binding instanceof Function) {
                    // subscribe so callback can properly invoked
                    instance[key].subscribe((value: any) => binding(value));
                } else {
                    instance[key] = binding;
                    // `ngOnChanges` won't be called on the component like this. Consider doing
                    // something like this:
                    // https://scm.edu-sharing.com/edu-sharing/projects/oeh-redaktion/ng-meta-widgets/-/blob/1603fb2dedadd3952401385bcbd91a4bd8407643/src/app/app.module.ts#L66-79
                }
            }
        }

        // 3. Get DOM element from component
        const domElem = (component.hostView as EmbeddedViewRef<any>).rootNodes[0] as HTMLElement;
        domElem.style.display = 'none';
        if (replace) {
            targetElement.innerHTML = null;
        }
        targetElement.appendChild(domElem);
        setTimeout(() => {
            domElem.style.display = null;
        }, delay);
        return component;
    }

    /**
     * returns true if the current browser is safari running
     */
    static isSafari() {
        return /AppleWebKit/.test(navigator.userAgent) && /Safari/.test(navigator.userAgent);
    }

    /**
     * dynamically inject an angular component into a regular html dom element
     * @param componentFactoryResolver The resolver service
     * @param viewContainerRef The viewContainerRef service
     * @param componentName The name of the angular component (e.g. SpinnerComponent)
     * @param targetElement The target element of the dom. If the element is null (not found), nothing is done
     * @param bindings Optional bindings (inputs & outputs) to the given component
     * @param delay Optional inflating delay in ms(some components may need some time to "init" the layout)
     * @param replace Whether to replace to previous `innerHTML` of `targetElement`
     * @param injector (to fetch templates for the component)
     */
    public static injectAngularComponent<T>(
        componentFactoryResolver: ComponentFactoryResolver,
        viewContainerRef: ViewContainerRef,
        componentName: Type<T>,
        targetElement: Element,
        bindings: { [key: string]: any } = null,
        { delay = 0, replace = true } = {},
        injector?: Injector,
    ): ComponentRef<T> {
        if (targetElement == null) {
            return null;
        }
        const factory = componentFactoryResolver.resolveComponentFactory(componentName);
        const component: ComponentRef<T> = viewContainerRef.createComponent(
            factory,
            undefined,
            injector,
        );
        if (bindings) {
            const instance: { [key: string]: any } = component.instance;
            for (const key in bindings) {
                const binding = bindings[key];
                if (binding instanceof Function) {
                    // subscribe so callback can properly invoked
                    instance[key].subscribe((value: any) => binding(value));
                } else {
                    instance[key] = binding;
                    // `ngOnChanges` won't be called on the component like this. Consider doing
                    // something like this:
                    // https://scm.edu-sharing.com/edu-sharing/projects/oeh-redaktion/ng-meta-widgets/-/blob/1603fb2dedadd3952401385bcbd91a4bd8407643/src/app/app.module.ts#L66-79
                }
            }
        }

        // 3. Get DOM element from component
        const domElem = (component.hostView as EmbeddedViewRef<any>).rootNodes[0] as HTMLElement;
        domElem.style.display = 'none';
        if (replace) {
            targetElement.innerHTML = null;
        }
        targetElement.appendChild(domElem);
        setTimeout(() => {
            domElem.style.display = null;
        }, delay);
        return component;
    }

    /**
     * smoothly scroll to the given child inside an element (The child will be placed around the first 1/3 of the parent's top)
     * @param child
     * @param element
     * @param smoothness
     */
    scrollSmoothElementToChild(child: Element, element: Element | 'auto' = 'auto', smoothness = 1) {
        let target: Element;
        if (element === 'auto') {
            let parent = child.parentElement;
            while (parent) {
                if (['scroll', 'auto'].includes(window.getComputedStyle(parent).overflowY)) {
                    target = parent;
                    break;
                }
                parent = parent.parentElement;
            }
        } else {
            target = element;
        }
        // y equals to the top of the child + any scrolling of the parent - the top of the parent
        let y =
            child.getBoundingClientRect().top +
            target.scrollTop -
            target.getBoundingClientRect().top;
        // move the focused element to 1/3 at the top of the container
        y += child.getBoundingClientRect().height / 2 - target.getBoundingClientRect().height / 3;
        return this.scrollSmoothElement(y, target, smoothness);
    }
    /**
     * Smoothly scrolls to the given y offset inside an element (use offsetTop on the child to
     * determine this position).
     *
     * @param smoothness lower numbers indicate less smoothness, higher more smoothness
     */
    scrollSmoothElement(pos: number = 0, element: Element, smoothness = 1, axis = 'y') {
        return new Promise<void>((resolve) => {
            this.ngZone.runOutsideAngular(() => {
                const currentPos = axis == 'x' ? element.scrollLeft : element.scrollTop;
                if (element.getAttribute('data-is-scrolling') == 'true') {
                    return;
                }
                const mode = currentPos > pos;
                let lastPos = pos;
                const maxPos =
                    axis == 'x'
                        ? element.scrollWidth - element.clientWidth
                        : element.scrollHeight - element.clientHeight;
                let limitReached = false;
                if (mode && pos <= 0) {
                    pos = 0;
                    limitReached = true;
                }
                if (!mode && pos >= maxPos) {
                    pos = maxPos;
                    limitReached = true;
                }
                let speed = 16;
                let last = new Date().getTime();
                const callback = () => {
                    let currentPos = axis == 'x' ? element.scrollLeft : element.scrollTop;
                    const posDiff = currentPos - lastPos;
                    const speedFactor = speed / 16;
                    const divider = (3 / speedFactor) * smoothness;
                    const minSpeed = (5 * speedFactor) / smoothness;
                    const maxSpeed = (50 * speedFactor) / smoothness;
                    lastPos = currentPos;
                    let finished = true;
                    if (currentPos > pos) {
                        currentPos -= Math.min(
                            maxSpeed,
                            Math.max((currentPos - pos) / divider, minSpeed),
                        );
                        finished = currentPos <= pos;
                    } else if (currentPos < pos && !mode) {
                        currentPos += Math.min(
                            maxSpeed,
                            Math.max((pos - currentPos) / divider, minSpeed),
                        );
                        finished = currentPos >= pos;
                    }
                    if (finished) {
                        currentPos = pos;
                    }

                    if (axis == 'x') {
                        element.scrollLeft = currentPos;
                    } else {
                        element.scrollTop = currentPos;
                    }
                    if (finished) {
                        element.removeAttribute('data-is-scrolling');
                        resolve();
                    } else {
                        speed = new Date().getTime() - last;
                        last = new Date().getTime();
                        window.requestAnimationFrame(callback);
                    }
                };
                window.requestAnimationFrame(callback);

                element.setAttribute('data-is-scrolling', 'true');
            });
        });
    }
    goToLogin(scope: string = null, next = window.location.href) {
        void this.injector
            .get(ConfigService)
            .get('loginUrl')
            .then((url: string) => {
                if (
                    url &&
                    !scope &&
                    !this.injector.get(ConfigService).instant('loginAllowLocal', false)
                ) {
                    window.location.href = url;
                    return;
                }
                void this.injector.get(Router).navigate([UIConstants.ROUTER_PREFIX + 'login'], {
                    queryParams: {
                        scope: scope,
                        next: next,
                    },
                });
            });
    }
}
