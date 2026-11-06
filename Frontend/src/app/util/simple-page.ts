import { SERVICE_UNAVAILABLE_ROUTE } from './service-unavailable';

/**
 * Routes of "simple pages": standalone pages that must work without the backend or any
 * global service of the app. On these, `AppComponent` renders only the router outlet and
 * skips its global initialization (translations, navigation, login checks, dialogs, ...).
 * A simple page is left by a full reload, so the decision is made once from the url.
 *
 * Relative to the base href. Add a route here together with its (standalone) component.
 */
export const SIMPLE_PAGE_ROUTES: readonly string[] = [SERVICE_UNAVAILABLE_ROUTE];

/**
 * @param route the current path relative to the base href, e.g. `components/error/503`
 */
export function isSimplePageRoute(route: string): boolean {
    const path = route.split(/[?#]/)[0].replace(/\/+$/, '');
    return SIMPLE_PAGE_ROUTES.includes(path);
}
