import { historyActionForKey } from './history-shortcut-util';

const press = (init: KeyboardEventInit): KeyboardEvent => new KeyboardEvent('keydown', init);

describe('historyActionForKey', () => {
    it('undoes and redoes with the US key positions', () => {
        void expect(historyActionForKey(press({ key: 'z', code: 'KeyZ', metaKey: true }))).toBe(
            'undo',
        );
        void expect(
            historyActionForKey(press({ key: 'z', code: 'KeyZ', metaKey: true, shiftKey: true })),
        ).toBe('redo');
        void expect(historyActionForKey(press({ key: 'y', code: 'KeyY', ctrlKey: true }))).toBe(
            'redo',
        );
    });

    // the key labelled Z on a German QWERTZ keyboard sits where US layouts have Y
    it('follows the character typed on a QWERTZ keyboard', () => {
        void expect(historyActionForKey(press({ key: 'z', code: 'KeyY', metaKey: true }))).toBe(
            'undo',
        );
        void expect(
            historyActionForKey(press({ key: 'Z', code: 'KeyY', metaKey: true, shiftKey: true })),
        ).toBe('redo');
        void expect(historyActionForKey(press({ key: 'y', code: 'KeyZ', metaKey: true }))).toBe(
            'redo',
        );
    });

    it('ignores the keys without Ctrl/Cmd or with Alt', () => {
        void expect(historyActionForKey(press({ key: 'z', code: 'KeyZ' }))).toBeNull();
        void expect(
            historyActionForKey(press({ key: 'z', code: 'KeyZ', metaKey: true, altKey: true })),
        ).toBeNull();
        void expect(
            historyActionForKey(press({ key: 'x', code: 'KeyX', metaKey: true })),
        ).toBeNull();
    });
});
