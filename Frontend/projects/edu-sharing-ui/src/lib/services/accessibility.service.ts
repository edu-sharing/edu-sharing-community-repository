import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { AccessibilitySettings } from '../types/accessibillity';
import { SessionStorageService } from 'ngx-edu-sharing-api';

@Injectable({
    providedIn: 'root',
})
export class AccessibilityService {
    private static readonly STORAGE_PREFIX = 'accessibility_';

    constructor(private storage: SessionStorageService) {}

    async set(accessibilitySettings: Partial<AccessibilitySettings>): Promise<void> {
        const currentValues = await this.observeAll().pipe(first()).toPromise();
        const values = Object.entries(accessibilitySettings)
            .filter(
                ([key, value]) =>
                    // check if value has been modified
                    (currentValues as any)[key] !== value,
            )
            .reduce((acc, [key, value]) => {
                acc[AccessibilityService.STORAGE_PREFIX + key] = value;
                return acc;
            }, {} as { [key: string]: any });
        if (Object.keys(values).length > 0) {
            return this.storage.setValues(values);
        }
    }

    observe<K extends keyof AccessibilitySettings>(key: K): Observable<AccessibilitySettings[K]>;
    observe<K extends keyof AccessibilitySettings>(
        keys: K[],
    ): Observable<Pick<AccessibilitySettings, K>>;
    observe(): Observable<AccessibilitySettings>;
    observe<K extends keyof AccessibilitySettings>(
        keyOrKeys?: K | K[],
    ): Observable<AccessibilitySettings[K] | Pick<AccessibilitySettings, K>> {
        if (typeof keyOrKeys === 'string') {
            return this.observeSingle(keyOrKeys);
        } else if (Array.isArray(keyOrKeys)) {
            return this.observeMultiple(keyOrKeys);
        } else {
            return this.observeAll();
        }
    }

    private observeSingle<K extends keyof AccessibilitySettings>(
        key: K,
    ): Observable<AccessibilitySettings[K]> {
        const defaultValues = new AccessibilitySettings();
        return this.storage.observe(AccessibilityService.STORAGE_PREFIX + key, defaultValues[key]);
    }

    private observeMultiple<K extends keyof AccessibilitySettings>(
        keys: K[],
    ): Observable<Pick<AccessibilitySettings, K>> {
        const defaultValues = new AccessibilitySettings();
        return combineLatest(
            keys.reduce((acc, key) => {
                acc[key] = this.storage.observe(
                    AccessibilityService.STORAGE_PREFIX + key,
                    defaultValues[key],
                );
                return acc;
            }, {} as { [key in keyof AccessibilitySettings]: Observable<AccessibilitySettings[key]> }),
        );
    }

    private observeAll(): Observable<AccessibilitySettings> {
        return this.observeMultiple(
            Object.keys(new AccessibilitySettings()) as Array<keyof AccessibilitySettings>,
        );
    }
}

/**
 * Rxjs.combineLatest on objects instead of arrays.
 *
 * This is a standard function in Rxjs 7, but we wrap it here for Rxjs 6.
 */
function combineLatest<T, K extends keyof T>(sources: {
    [key in K]: Observable<T[key]>;
}): Observable<T> {
    return rxjs
        .combineLatest(
            Object.entries(sources).map(([key, source]) =>
                (source as Observable<T[K]>).pipe(map((result) => ({ key, result }))),
            ),
        )
        .pipe(
            map((results) =>
                results.reduce((acc, { key, result }) => {
                    acc[key as K] = result;
                    return acc;
                }, {} as T),
            ),
        );
}
