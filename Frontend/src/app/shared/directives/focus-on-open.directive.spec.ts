import { InputModalityDetector } from '@angular/cdk/a11y';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FocusOnOpenDirective } from './focus-on-open.directive';

/** A trigger outside the panel and the panel itself, which shows some content while open. */
@Component({
    template: `
        <button class="trigger" type="button">Trigger</button>
        <button class="other" type="button">Other</button>
        <div class="panel" tabindex="-1" [esFocusOnOpen]="open" [esFocusOnOpenContent]="content">
            <button class="inside" type="button">Inside</button>
        </div>
    `,
    standalone: false,
})
class HostComponent {
    open = false;
    content: object | null = null;
}

describe('FocusOnOpenDirective', () => {
    let fixture: ComponentFixture<HostComponent>;
    let host: HostComponent;
    let modality: 'keyboard' | 'mouse';

    const el = (selector: string) => fixture.nativeElement.querySelector(selector) as HTMLElement;

    beforeEach(() => {
        modality = 'keyboard';
        TestBed.configureTestingModule({
            declarations: [HostComponent, FocusOnOpenDirective],
            providers: [
                {
                    provide: InputModalityDetector,
                    useValue: {
                        get mostRecentModality() {
                            return modality;
                        },
                    },
                },
            ],
        });
        fixture = TestBed.createComponent(HostComponent);
        host = fixture.componentInstance;
        document.body.appendChild(fixture.nativeElement);
        fixture.detectChanges();
    });

    afterEach(() => fixture.nativeElement.remove());

    function update(changes: Partial<HostComponent>): void {
        Object.assign(host, changes);
        fixture.detectChanges();
    }

    /** A key press, as the user activating something that changes the panel's content. */
    function pressKey(target: HTMLElement = document.activeElement as HTMLElement): void {
        target.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    }

    it('focuses the panel when it opens after keyboard use', () => {
        el('.trigger').focus();
        update({ open: true, content: {} });
        expect(document.activeElement).toBe(el('.panel'));
    });

    it('does not move focus when opened with the mouse', () => {
        modality = 'mouse';
        el('.trigger').focus();
        update({ open: true, content: {} });
        expect(document.activeElement).toBe(el('.trigger'));
    });

    it('focuses the panel again when a key press gives the open panel new content', () => {
        update({ open: true, content: {} });
        el('.other').focus();
        pressKey();
        update({ content: {} });
        expect(document.activeElement).toBe(el('.panel'));
    });

    it('leaves focus alone when code changes the content without a recent key press', () => {
        update({ open: true, content: {} });
        el('.other').focus();
        update({ content: {} });
        expect(document.activeElement).toBe(el('.other'));
    });

    it('keeps focus inside the panel when its content changes', () => {
        update({ open: true, content: {} });
        el('.inside').focus();
        pressKey();
        update({ content: {} });
        expect(document.activeElement).toBe(el('.inside'));
    });

    it('ignores content being cleared', () => {
        update({ open: true, content: {} });
        el('.other').focus();
        pressKey();
        update({ content: null });
        expect(document.activeElement).toBe(el('.other'));
    });

    it('returns focus to the element that triggered the latest content on close', () => {
        el('.trigger').focus();
        update({ open: true, content: {} });
        el('.other').focus();
        pressKey();
        update({ content: {} });
        expect(document.activeElement).toBe(el('.panel'));
        update({ open: false, content: null });
        expect(document.activeElement).toBe(el('.other'));
    });

    it('keeps the earlier return target when new content arrives without meaningful focus', () => {
        el('.trigger').focus();
        update({ open: true, content: {} });
        (document.activeElement as HTMLElement).blur();
        pressKey(document.body);
        update({ content: {} });
        el('.panel').focus();
        update({ open: false, content: null });
        expect(document.activeElement).toBe(el('.trigger'));
    });

    describe('with an overlay menu', () => {
        let overlay: HTMLElement;
        let menuItem: HTMLButtonElement;

        beforeEach(() => {
            overlay = document.createElement('div');
            overlay.className = 'cdk-overlay-container';
            menuItem = document.createElement('button');
            overlay.appendChild(menuItem);
            document.body.appendChild(overlay);
        });

        afterEach(() => overlay.remove());

        function openMenuFrom(trigger: HTMLElement): void {
            trigger.setAttribute('aria-haspopup', 'menu');
            trigger.setAttribute('aria-expanded', 'true');
            menuItem.focus();
        }

        it('counts a menu opened from inside the panel as inside', () => {
            update({ open: true, content: {} });
            openMenuFrom(el('.inside'));
            pressKey();
            update({ content: {} });
            expect(document.activeElement).toBe(menuItem);
        });

        it('returns focus to the trigger of a menu opened outside the panel', () => {
            update({ open: true, content: {} });
            openMenuFrom(el('.other'));
            pressKey();
            update({ content: {} });
            expect(document.activeElement).toBe(el('.panel'));
            menuItem.remove();
            el('.other').removeAttribute('aria-expanded');
            update({ open: false, content: null });
            expect(document.activeElement).toBe(el('.other'));
        });
    });
});
