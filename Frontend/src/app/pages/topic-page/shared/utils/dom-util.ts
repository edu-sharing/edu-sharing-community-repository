/**
 * Retrieves the height of the body element.
 */
export const getBodyHeight = (): number => {
    // reference: https://stackoverflow.com/a/1147768
    const body: HTMLElement = document.body;
    const html: HTMLElement = document.documentElement;

    return Math.max(
        body.clientHeight,
        body.scrollHeight,
        body.offsetHeight,
        html.clientHeight,
        html.scrollHeight,
        html.offsetHeight,
    );
};

/**
 * Scrolls a given HTMLElement into view.
 *
 * @param element
 */
export const scrollIntoView = (element: HTMLElement): void => {
    element.scrollIntoView({
        inline: 'start',
        block: 'nearest',
        behavior: 'smooth',
    });
};
