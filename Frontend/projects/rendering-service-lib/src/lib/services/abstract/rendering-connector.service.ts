import { Node } from 'ngx-edu-sharing-api';

/**
 * Bridge to the host's connector handling (e.g. ONLYOFFICE, LTI tools), used to offer opening
 * material the browser can not display itself.
 *
 * Provided by the edu-sharing app only. Hosts without connector support (e.g. the rendering
 * web component) do not provide it, so it must be injected optionally.
 */
export abstract class RenderingConnectorService {
    /** whether the given node can be opened with a connector by the current user */
    abstract hasAvailableConnector(node: Node): Promise<boolean>;
    /** opens the given node with its connector (in a new window) */
    abstract openConnector(node: Node): Promise<void>;
}
