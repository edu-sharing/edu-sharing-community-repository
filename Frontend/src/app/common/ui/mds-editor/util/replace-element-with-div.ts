export function replaceElementWithDiv(element: Element): HTMLDivElement {
    const div = document.createElement('div');
    div.setAttribute('data-property', element.localName);
    element.parentNode.replaceChild(div, element);
    return div;
}