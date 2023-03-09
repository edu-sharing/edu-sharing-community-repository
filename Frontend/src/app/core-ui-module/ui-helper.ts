import {OPEN_URL_MODE, UIConstants} from '../core-module/ui/ui-constants';
import {ConfigurationService} from '../core-module/rest/services/configuration.service';
import {TranslateService} from '@ngx-translate/core';
import {ActivatedRoute, NavigationExtras, Router} from '@angular/router';
import {
    Collection, CollectionReference,
    Connector,
    Filetype,
    LoginResult,
    MdsInfo,
    Node,
    NodeLock,
    NodeWrapper,
    ParentList,
    Permission,
} from '../core-module/rest/data-object';
import {RestConstants} from '../core-module/rest/rest-constants';
import {RestNodeService} from '../core-module/rest/services/rest-node.service';
import {Toast} from './toast';
import {RestHelper} from '../core-module/rest/rest-helper';
import {TemporaryStorageService} from '../core-module/rest/services/temporary-storage.service';
import {UIService} from '../core-module/rest/services/ui.service';
import {
    ComponentFactoryResolver,
    ComponentRef,
    ElementRef,
    EmbeddedViewRef,
    EventEmitter,
    Injector,
    NgZone,
    Type,
    ViewContainerRef,
} from '@angular/core';
import {RestCollectionService} from '../core-module/rest/services/rest-collection.service';
import {RestConnectorsService} from '../core-module/rest/services/rest-connectors.service';
import {FrameEventsService} from '../core-module/rest/services/frame-events.service';
import {ListItem} from '../core-module/ui/list-item';
import {BridgeService} from '../core-bridge-module/bridge.service';
import {OptionItem} from './option-item';
import {RestConnectorService} from '../core-module/rest/services/rest-connector.service';
import {Observable, Observer, of} from 'rxjs';
import {RouterHelper} from './router.helper';
import {PlatformLocation} from '@angular/common';
import {MessageType} from '../core-module/ui/message-type';
import {Helper} from '../core-module/rest/helper';
import {NodeHelperService} from './node-helper.service';
import { RestIamService } from '../core-module/rest/services/rest-iam.service';
import { DialogButton } from '../core-module/ui/dialog-button';
import {catchError} from "rxjs/operators";
import {ObservedValueOf} from "rxjs/internal/types";

export class UIHelper {
    public static evaluateMediaQuery(type: string, value: number) {
        if (type == UIConstants.MEDIA_QUERY_MAX_WIDTH)
            return value > window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MIN_WIDTH)
            return value < window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MAX_HEIGHT)
            return value > window.innerHeight;
        if (type == UIConstants.MEDIA_QUERY_MIN_HEIGHT)
            return value < window.innerHeight;
        console.warn('Unsupported media query ' + type);
        return true;
    }

    public static getBlackWhiteContrast(color: string) {}
    static changeQueryParameter(
        router: Router,
        route: ActivatedRoute,
        name: string,
        value: any,
    ) {
        route.queryParams.subscribe((data: any) => {
            let queryParams: any = {};
            for (let key in data) {
                queryParams[key] = data[key];
            }
            queryParams[name] = value;
            router.navigate([], { queryParams: queryParams });
        });
    }

    /**
     * The materialize textarea should auto-adjust height based on content
     * however, ngmodel does not refresh the element. This method will simulate a keyboard event to refresh the state
     * It will wait for the element to come active and send a keyboard event
     * Using ng's ElementRef will not work :/ We have to use a global dom id
     * @param {ElementRef} element
     */
    static invalidateMaterializeTextarea(id: string, timeout = 10) {
        setTimeout(() => {
            if (document.getElementById(id) == null) {
                UIHelper.invalidateMaterializeTextarea(id, 100);
                return;
            }
            let event = new KeyboardEvent('keyup', {
                view: window,
                bubbles: true,
                cancelable: true,
            });
            document.getElementById(id).dispatchEvent(event);
        }, timeout);
    }

    /**
     * returns true if the given string seems to be an email
     * @param {string} email
     */
    static isEmail(mail: string) {
        if (!mail) return false;
        if (mail.trim()) {
            const EMAIL_REGEXP = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            if (mail && !EMAIL_REGEXP.test(mail)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * returns an factor indicating the strength of a password
     * Higher values mean better password strength
     * @param password
     */
    private static getPasswordStrength(password: string) {
        let strength: number;
        // These are weighting factors
        let flc = 1.0; // lowercase factor
        let fuc = 1.0; // uppercase factor
        let fnm = 1.3; // number factor
        let fsc = 1.5; // special char factor

        let regex_sc = new RegExp(/[!@#\$%\^\&*\)\(+=._-]/g);

        let lcase_count: any = password.match(/[a-z]/g);
        lcase_count = lcase_count ? lcase_count.length : 0;
        let ucase_count: any = password.match(/[A-Z]/g);
        ucase_count = ucase_count ? ucase_count.length : 0;
        let num_count: any = password.match(/[0-9]/g);
        num_count = num_count ? num_count.length : 0;
        let schar_count: any = password.match(regex_sc);
        schar_count = schar_count ? schar_count.length : 0;
        strength =
            ((lcase_count * flc + 1) *
                (ucase_count * fuc + 1) *
                (num_count * fnm + 1) *
                (schar_count * fsc + 1))
            // / (avg + 1);

        // console.log('Strengt: '+strength);
        return strength;
    }
    /**
     * returns an factor indicating the repeat of signd in a password
     * Higher values mean better password strength
     * @param password
     */
    private static detectPW(password: string) {
        let pw_parts = password.split('');
        let i;
        let ords = new Array();
        for (i in pw_parts) {
            ords[i] = pw_parts[i].charCodeAt(0);
        }
        let accum = 0;
        let lasti = ords.length - 1;

        for (let i = 0; i < lasti; ++i) {
            accum += Math.abs(ords[i] - ords[i + 1]);
        }
        // console.log('detect: '+accum/lasti);
        return accum / lasti;
    }

    /**
     * returns the password strength as a string value
     * weak, accept, medium, strong
     * @param password
     */
    public static getPasswordStrengthString(password: string) {
        let min_length = 5;
        // console.log("strength: "+this.getPasswordStrength(password));
        if (password && password.length >= min_length) {
            if (this.getPasswordStrength(password) > 70) {
                if (this.getPasswordStrength(password) > 160) {
                    return 'strong';
                } else {
                    return 'medium';
                }
            } else {
                return 'accept';
            }
        } else {
            return 'weak';
        }
    }

    static routeToSearchNode(router: Router, reurl: string, node: Node) {
        let converted = UIHelper.convertSearchParameters(node);
        router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
            queryParams: {
                query: converted.query,
                reurl: reurl,
                repository:
                    node.properties[
                        RestConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY
                    ],
                mds: node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_MDS],
                parameters: JSON.stringify(converted.parameters),
            },
        });
    }
    public static goToCollection(
        router: Router,
        node: Node,
        mode: null | 'new' | 'edit' = null,
        extras: NavigationExtras = {},
    ) {
        if(mode === 'new' || mode === 'edit') {
            router.navigate([UIConstants.ROUTER_PREFIX, 'collections', 'collection', mode, node.ref.id], extras);
        } else {
            extras.queryParams = { id: node.ref.id };
            router.navigate([UIConstants.ROUTER_PREFIX, 'collections'], extras);
        }
    }
    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param node The node to open and show
     */
    public static goToWorkspace(
        nodeService: RestNodeService,
        router: Router,
        login: LoginResult,
        node: Node,
        extras: NavigationExtras = {},
    ) {
        nodeService
            .getNodeParents(node.ref.id)
            .subscribe((data: ParentList) => {
                extras.queryParams = {
                    id: node.parent.id,
                    file: node.ref.id,
                    root: data.scope,
                };
                router.navigate(
                    [
                        UIConstants.ROUTER_PREFIX +
                            'workspace/' +
                            (login.currentScope ? login.currentScope : 'files'),
                    ],
                    extras,
                );
            });
    }
    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param folder The folder id to open
     */
    public static goToWorkspaceFolder(
        nodeService: RestNodeService,
        router: Router,
        login: LoginResult,
        folder: string,
        extras: NavigationExtras = {},
    ) {
        extras.queryParams = { id: folder };
        router.navigate(
            [
                UIConstants.ROUTER_PREFIX +
                    'workspace/' +
                    (login && login.currentScope
                        ? login.currentScope
                        : 'files'),
            ],
            extras,
        );
    }
    static convertSearchParameters(node: Node) {
        let parameters = JSON.parse(
            node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS],
        );
        let result: any = { parameters: {}, query: null };
        for (let parameter of parameters) {
            if (parameter.property == RestConstants.PRIMARY_SEARCH_CRITERIA) {
                if (parameter.values[0] == '*') parameter.values[0] = '';
                result.query = parameter.values[0];
                continue;
            }
            result.parameters[parameter.property] = parameter.values;
        }
        return result;
    }

    static materializeSelect() {
        eval("$('select').css('display','none');$('select').material_select()");
    }

    static showAddedToCollectionInfo(
        bridge: BridgeService,
        router: Router,
        node: any,
        count: number,
    ) {
        let scope = node.collection ? node.collection.scope : node.scope;
        let type = node.collection ? node.collection.type : node.type;
        if (scope == RestConstants.COLLECTIONSCOPE_MY) {
            scope = 'MY';
        } else if (
            scope == RestConstants.COLLECTIONSCOPE_ORGA ||
            scope == RestConstants.COLLECTIONSCOPE_CUSTOM
        ) {
            scope = 'SHARED';
        } else if (
            scope == RestConstants.COLLECTIONSCOPE_ALL ||
            scope == RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC
        ) {
            scope = 'PUBLIC';
        } else if (type == RestConstants.COLLECTIONTYPE_EDITORIAL) {
            scope = 'PUBLIC';
        } else if (type == RestConstants.COLLECTIONTYPE_MEDIA_CENTER) {
            scope = 'MEDIA_CENTER';
        }
        bridge.showTemporaryMessage(MessageType.info,
            'WORKSPACE.TOAST.ADDED_TO_COLLECTION_' + scope,
            { count: count, collection: RestHelper.getTitle(node) },
            {
                link: {
                    caption: 'WORKSPACE.TOAST.VIEW_COLLECTION',
                    callback: () => UIHelper.goToCollection(router, node),
                },
            },
        );
    }

    static prepareMetadatasets(
        translate: TranslateService,
        mdsSets: MdsInfo[],
    ) {
        for (let i = 0; i < mdsSets.length; i++) {
            if (mdsSets[i].id == 'mds')
                mdsSets[i].name = translate.instant('DEFAULT_METADATASET', {
                    name: mdsSets[i].name,
                });
        }
    }

    /**
     * handles adding nodes to a collection
     * @param nodeHelper
     * @param collectionService
     * @param router
     * @param bridge
     * @param collection
     * @param nodes
     * @param callback
     * @param allowDuplicate false (default) will trigger a confirmation if duplicate was detected, true will always create a duplicate and 'ignore' works as false but will not trigger a confirmation but silently abort
     */
    static addToCollection(
        nodeHelper: NodeHelperService,
        collectionService: RestCollectionService,
        router: Router,
        bridge: BridgeService,
        collection: Node,
        nodes: Node[],
        callback: (nodes: CollectionReference[]) => void = null,
        allowDuplicate: boolean | 'ignore' = false,
    ) {
        Observable.forkJoin(nodes.map(node =>
            collectionService.addNodeToCollection(
                collection.ref.id,
                node.ref.id,
                node.ref.repo,
                allowDuplicate === true
                ).pipe(
                    catchError(error => of({error, node}),)
                )
        )).subscribe((results) => {
            const success: NodeWrapper[] = results.filter((r) => !(r as any).error);
            const failed: { node: Node, error: any }[] = (
                results.filter((r) => !!(r as any).error) as {node: Node, error: any}[]
            );
            if(success.length > 0) {
                UIHelper.showAddedToCollectionInfo(
                    bridge,
                    router,
                    collection,
                    success.length,
                );
            }
            if(failed.length > 0) {
                const duplicated = failed.filter(({error}) => error.status === RestConstants.DUPLICATE_NODE_RESPONSE);
                if (duplicated.length > 0) {
                    if(allowDuplicate !== 'ignore') {
                        bridge.showModalDialog({
                            title: 'COLLECTIONS.ADD_TO.DUPLICATE_TITLE',
                            message: 'COLLECTIONS.ADD_TO.DUPLICATE_MESSAGE',
                            messageParameters: {count: duplicated.length},
                            isCancelable: true,
                            buttons: DialogButton.getYesNo(
                                () => {
                                    bridge.closeModalDialog();
                                    if (callback) {
                                        callback(results.map(n => n.node as CollectionReference));
                                    }
                                },
                                () => {
                                    bridge.closeModalDialog();
                                    UIHelper.addToCollection(
                                        nodeHelper,
                                        collectionService,
                                        router,
                                        bridge,
                                        collection,
                                        duplicated.map(d => d.node),
                                        callback,
                                        true
                                    )
                                }
                            )
                        });
                        return;
                    }
                } else {
                    nodeHelper.handleNodeError(
                        RestHelper.getTitle(failed[0].node),
                        failed[0].error,
                    );
                }
            }

            if (callback) {
                callback(success.map(n => n.node as CollectionReference));
            }
        });
    }
    static openConnector(
        connector: RestConnectorsService,
        iam: RestIamService,
        events: FrameEventsService,
        toast: Toast,
        node: Node,
        type: Filetype = null,
        win: any = null,
        connectorType: Connector = null,
        newWindow = true,
    ) {
        if (connectorType == null) {
            connectorType = connector.connectorSupportsEdit(node);
        }
        let isCordova = connector
            .getRestConnector()
            .getBridgeService()
            .isRunningCordova();
        if (win == null && newWindow) {
            win = UIHelper.getNewWindow(connector.getRestConnector());
        }

        connector.nodeApi.isLocked(node.ref.id).subscribe(
            (result: NodeLock) => {
                if (result.isLocked) {
                    toast.error(null, 'TOAST.NODE_LOCKED');
                    if (win) win.close();
                    return;
                }
                iam.getCurrentUserAsync().then(
                    user => {
                        if (
                            user.person.quota.enabled &&
                            user.person.quota.sizeCurrent >=
                                user.person.quota.sizeQuota
                        ) {
                            toast.showModalDialog(
                                'CONNECTOR_QUOTA_REACHED_TITLE',
                                'CONNECTOR_QUOTA_REACHED_MESSAGE',
                                DialogButton.getOk(() => {
                                    toast.closeModalDialog();
                                }),
                                true,
                            );
                            if (win) win.close();
                            return;
                        }
                        connector
                            .generateToolUrl(connectorType, type, node)
                            .subscribe(
                                (url: string) => {
                                    if (win) {
                                        win.location.href = url;
                                    } else if (isCordova) {
                                        UIHelper.openUrl(
                                            url,
                                            connector
                                                .getRestConnector()
                                                .getBridgeService(),
                                            OPEN_URL_MODE.Blank,
                                        );
                                    } else {
                                        window.location.replace(url);
                                    }
                                    if (win) {
                                        events.addWindow(win);
                                    }
                                },
                                error => {
                                    toast.error(null, error);
                                    if (win) win.close();
                                },
                            );
                    },
                    error => {
                        toast.error(null, error);
                        if (win) win.close();
                    },
                );
            },
            (error: any) => {
                toast.error(error);
                if (win) win.close();
            },
        );
    }

    /**
     * smoothly scroll to the given y offset
     * @param {y} number
     * @param {smoothness} lower numbers indicate less smoothness, higher more smoothness
     */
    static scrollSmooth(y: number = 0, smoothness = 1) {
        let mode = window.scrollY >= y;
        let divider = 3 * smoothness;
        let minSpeed = 7 / smoothness;
        let lastY = y;
        let interval = setInterval(() => {
            let yDiff = window.scrollY - lastY;
            lastY = window.scrollY;
            if (window.scrollY > y && mode && yDiff) {
                window.scrollBy(
                    0,
                    -Math.max((window.scrollY - y) / divider, minSpeed),
                );
            } else if (window.scrollY < y && !mode && yDiff) {
                window.scrollBy(
                    0,
                    Math.max((y - window.scrollY) / divider, minSpeed),
                );
            } else {
                clearInterval(interval);
            }
        }, 16);
    }
    /**
     * smoothly scroll to the given y offset inside an element (use offsetTop on the child to determine this position)
     * @param {y} number
     * @param {smoothness} lower numbers indicate less smoothness, higher more smoothness
     */
    static scrollSmoothElement(
        pos: number = 0,
        element: Element,
        smoothness = 1,
        axis = 'y',
    ) {
        return new Promise(resolve => {
            let currentPos =
                axis == 'x' ? element.scrollLeft : element.scrollTop;
            if (element.getAttribute('data-is-scrolling') == 'true') {
                return;
            }
            let mode = currentPos > pos;
            let divider = 3 * smoothness;
            let minSpeed = 7 / smoothness;
            let lastPos = pos;
            let maxPos =
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
            let interval = setInterval(() => {
                let currentPos =
                    axis == 'x' ? element.scrollLeft : element.scrollTop;
                let posDiff = currentPos - lastPos;
                lastPos = currentPos;
                let finished = true;
                if (currentPos > pos) {
                    currentPos -= Math.max(
                        (currentPos - pos) / divider,
                        minSpeed,
                    );
                    finished = currentPos <= pos;
                } else if (currentPos < pos && !mode) {
                    currentPos += Math.max(
                        (pos - currentPos) / divider,
                        minSpeed,
                    );
                    finished = currentPos >= pos;
                }
                if (axis == 'x') element.scrollLeft = currentPos;
                else element.scrollTop = currentPos;
                if (finished) {
                    clearInterval(interval);
                    element.removeAttribute('data-is-scrolling');
                    resolve();
                }
            }, 16);
            element.setAttribute('data-is-scrolling', 'true');
        });
    }

    /**
     * smoothly scroll to the given child inside an element (The child will be placed around the first 1/3 of the parent's top)
     * @param child
     * @param element
     * @param smoothness
     */
    static scrollSmoothElementToChild(
        child: Element,
        element: Element,
        smoothness = 1,
    ) {
        // y equals to the top of the child + any scrolling of the parent - the top of the parent
        let y =
            child.getBoundingClientRect().top +
            element.scrollTop -
            element.getBoundingClientRect().top;
        // move the focused element to 1/3 at the top of the container
        y +=
            child.getBoundingClientRect().height / 2 -
            element.getBoundingClientRect().height / 3;
        this.scrollSmoothElement(y, element, smoothness);
    }

    static setFocusOnCard() {
        let elements = document
            .getElementsByClassName('card')[0]
            .getElementsByTagName('*');
        this.focusElements(elements);
    }
    static setFocusOnDropdown(ref: ElementRef) {
        // the first element(s) might be currently invisible, so try to focus from bottom to top
        if (ref && ref.nativeElement) {
            let elements = ref.nativeElement.getElementsByTagName('a');
            this.focusElements(elements);
        }
    }

    private static focusElements(elements: any) {
        for (let i = elements.length - 1; i >= 0; i--) {
            elements[i].focus();
        }
    }

    static getDefaultCollectionColumns() {
        let columns = [];
        columns.push(new ListItem('COLLECTION', 'title'));
        columns.push(new ListItem('COLLECTION', 'info'));
        columns.push(new ListItem('COLLECTION', 'scope'));
        return columns;
    }

    static goToDefaultLocation(
        router: Router,
        platformLocation: PlatformLocation,
        configService: ConfigurationService,
        replaceUrl = false,
    ) {
        let defaultLocation = configService.instant('loginDefaultLocation', 'workspace');
        if(!defaultLocation.match(/https?:\/\/*/)) {
            defaultLocation = UIConstants.ROUTER_PREFIX + defaultLocation;
        }
        RouterHelper.navigateToAbsoluteUrl(
            platformLocation,
            router,
            defaultLocation,
            replaceUrl,
        );
    }

    static openUrl(
        url: string,
        bridge: BridgeService,
        mode = OPEN_URL_MODE.Current,
    ) {
        if (bridge.isRunningCordova()) {
            if (mode == OPEN_URL_MODE.BlankSystemBrowser) {
                return bridge.getCordova().openBrowser(url);
            } else {
                return bridge.getCordova().openInAppBrowser(url);
            }
        } else {
            if (mode == OPEN_URL_MODE.Current) {
                window.location.href = url;
                return;
            } else {
                return window.open(url, '_blank');
            }
        }
    }

    static filterValidOptions(ui: UIService, options: OptionItem[]) {
        if (options == null) return null;
        options = options.filter(value => value != null);
        let optionsFiltered: OptionItem[] = [];
        for (let option of options) {
            if (
                (!option.onlyMobile || (option.onlyMobile && ui.isMobile())) &&
                (!option.onlyDesktop ||
                    (option.onlyDesktop && !ui.isMobile())) &&
                (!option.mediaQueryType ||
                    (option.mediaQueryType &&
                        UIHelper.evaluateMediaQuery(
                            option.mediaQueryType,
                            option.mediaQueryValue,
                        )))
            )
                optionsFiltered.push(option);
        }
        return optionsFiltered;
    }
    static filterToggleOptions(options: OptionItem[], toggle: boolean) {
        let result: OptionItem[] = [];
        for (let option of options) {
            if (option.isToggle == toggle) result.push(option);
        }
        return result;
    }

    /**
     * open a window (blank) to prevent popup blocking
     * @param {RestConnectorService} connector
     * @returns {any}
     */
    public static getNewWindow(connector: RestConnectorService) {
        if (connector.getBridgeService().isRunningCordova()) return null;
        return window.open('');
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
    ): ComponentRef<T> {
        if (targetElement == null) {
            return null;
        }
        const factory = componentFactoryResolver.resolveComponentFactory(
            componentName,
        );
        const component: ComponentRef<T> = viewContainerRef.createComponent(
            factory
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
                }
            }
        }

        // 3. Get DOM element from component
        const domElem = (component.hostView as EmbeddedViewRef<any>)
            .rootNodes[0] as HTMLElement;
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
     * returns common active route parameters that should be keeped
     * currently including: mainnav,
     * @param route
     */
    static getCommonParameters(route: ActivatedRoute) {
        const COPY_PARAMS = ['mainnav', 'reurl', 'applyDirectories'];
        return new Observable<any>((observer: Observer<any>) => {
            route.queryParams
                .pipe()
                .first()
                .subscribe(queryParams => {
                    let result: any = {};
                    COPY_PARAMS.forEach(params => {
                        if (queryParams[params]) {
                            result[params] = queryParams[params];
                        }
                    });
                    observer.next(result);
                    observer.complete();
                });
        });
    }

    /**
     * returns true if the error message includes the given string
     * @param error
     * @param {string} data
     * @returns {boolean}
     */
    static errorContains(error: any, data: string) {
        try {
            return error.error.message.indexOf(data) != -1;
        } catch (e) {}
        return false;
    }

    /**
     * waits until the given component/object is not null and available
     * @param ngZone NgZone Service
     * @param clz the class where the component is attached (usually "this")
     * @param componentName The name of the property
     */
    static waitForComponent(ngZone: NgZone, clz: any, componentName: string) {
        return ngZone.runOutsideAngular(() => {
            return new Observable((observer: Observer<any>) => {
                let interval = setInterval(() => {
                    if (clz[componentName]) {
                        observer.next(clz[componentName]);
                        observer.complete();
                        clearInterval(interval);
                    }
                    else if(!clz) {
                        clearInterval(interval);
                    }
                }, 1000 / 60);
            });
        });
    }

    //http://stackoverflow.com/questions/25099409/copy-to-clipboard-as-plain-text
    static copyElementToClipboard(input: HTMLTextAreaElement) {
        input.focus();
        input.select();
        document.execCommand('SelectAll');
        document.execCommand('Copy', false, null);
    }
    static copyToClipboard(text: string) {
        let input = document.createElement(
            'textarea',
        ) as HTMLTextAreaElement;
        input.innerHTML = text;
        document.body.appendChild(input);
        UIHelper.copyElementToClipboard(input);
        document.body.removeChild(input);
    }


    static mergePermissions(source: Permission[], add: Permission[]) {
        const merge = source;
        for (const p2 of add) {
            // do only add new, unique permissions
            if (merge.filter(p1 => Helper.objectEquals(p1, p2)).length === 0) {
                merge.push(p2);
            }
        }
        return merge;
    }
    /**
     * merge two permission sets
     * If a user/group is duplicated, the one with the highest permission will win
     * Consumer < Collaborator < Coordinator
     * @param source
     * @param add
     */
    static mergePermissionsWithHighestPermission(
        source: Permission[],
        add: Permission[],
    ) {
        const result = Helper.deepCopyArray(source);
        for (const p2 of add) {
            const map = source.filter(
                s =>
                    s.authority.authorityName === p2.authority.authorityName &&
                    s.authority.authorityType === s.authority.authorityType,
            );
            if (map.length === 1) {
                const perm1 = map[0].permissions.filter(
                    p => RestConstants.BASIC_PERMISSIONS.indexOf(p) !== -1,
                );
                const perm2 = p2.permissions.filter(
                    p => RestConstants.BASIC_PERMISSIONS.indexOf(p) !== -1,
                );
                if (UIHelper.permissionIsGreaterThan(perm2[0], perm1[0])) {
                    result.splice(result.indexOf(map[0]), 1);
                    result.push(p2);
                }
            } else {
                // add new permission to list
                result.push(p2);
            }
        }
        return result;
    }
    static permissionIsGreaterThan(p1: string, p2: string) {
        return (
            RestConstants.BASIC_PERMISSIONS.indexOf(p1) >
            RestConstants.BASIC_PERMISSIONS.indexOf(p2)
        );
    }
}
