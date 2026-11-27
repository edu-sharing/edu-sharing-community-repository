/**
 * Bridge to the host for links the rendering opens in a new tab ("open in new tab" of the html
 * module).
 *
 * Provided by the edu-sharing app only: when running as mobile app it opens links to the own
 * platform in the in-app browser, which shares the session (a plain new window would leave the
 * app for the system browser, without the session). Hosts that do not provide it (e.g. the
 * rendering web component) keep the default link behavior, so it must be injected optionally.
 */
export abstract class RenderingLinkService {
    /**
     * Opens the given url in a new tab, or the host's equivalent of it.
     * @returns true if the host opened it (the caller must then skip its default behavior),
     * false to let the caller open it as usual (e.g. external urls)
     */
    abstract openInNewTab(url: string): boolean;
}

/**
 * Click handler for `<a [href] target="_blank">` links: hands the link to the host's
 * {@link RenderingLinkService} and suppresses the browser's default navigation if it was handled.
 * The link keeps its href (keyboard, context menu, copying the address).
 */
export function handleNewTabLinkClick(
    event: MouseEvent,
    url: string | null | undefined,
    linkService: RenderingLinkService | null,
): void {
    if (url && linkService?.openInNewTab(url)) {
        event.preventDefault();
    }
}
