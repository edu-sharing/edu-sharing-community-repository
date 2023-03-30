import { Injectable } from '@angular/core';
import { KeyboardShortcut } from '../types/keyboard-shortcuts';
import { Observable } from 'rxjs';

@Injectable()
export abstract class KeyboardShortcutsService {
    abstract register(shortcuts: KeyboardShortcut[], { until }: { until: Observable<void> }): void;
}
