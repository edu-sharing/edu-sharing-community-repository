import { Injectable } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
import { notNull } from '../../util/functions';
import { NavigationScheduler } from './navigation-scheduler';

/**
 * Provides a factory for user-modifiable values.
 *
 * The concept is to provide a value that has a system default, can be changed by the user, or be
 * overridden by the system, while keeping track of each value explicitly.
 *
 * We provide methods for controlling the user value with (reactive) forms and for tracking the
 * user value as query parameter.
 */
@Injectable({ providedIn: 'root' })
export class UserModifiableValuesService {
    constructor(
        navigationScheduler: NavigationScheduler,
        //  preferences: SessionStorageService
    ) {
        UserModifiableValue.navigationScheduler = navigationScheduler;
        // UserModifiableValue.preferences = preferences;
    }

    createDict(systemValue?: { [key: string]: any }): UserModifiableValue<{ [key: string]: any }> {
        return new UserModifiableValue(userModifiableDictType, systemValue);
    }

    createString<S extends string>(systemValue?: S): UserModifiableValue<S> {
        return new UserModifiableValue(new UserModifiableStringType<S>(), systemValue);
    }

    createMapped<T>(mapping: Mapping<T>, systemValue?: T): UserModifiableValue<T> {
        return new UserModifiableValue<T>(new UserModifiableMappedType(mapping), systemValue);
    }

    createBoolean(systemValue?: boolean): UserModifiableValue<boolean> {
        return new UserModifiableValue(userModifiableBooleanType, systemValue);
    }
}

/**
 * Represents a value that has a system default and can be modified by the user.
 *
 * Allows to sync the value with a query parameter such that only values changed by the user will be
 * reflected in the query parameters.
 */
export class UserModifiableValue<T> {
    static navigationScheduler: NavigationScheduler;
    // static preferences: SessionStorageService;

    /**
     * Shorthand for registration with two-way bindings, e.g.,
     *
     *      [(someVariable)]="someUserModifiableValue.value"
     */
    get value(): T {
        return this.getValue();
    }
    set value(value: T) {
        this.setUserValue(value);
    }

    private _systemValue = new BehaviorSubject<T>(this._type.null);
    private _userValue = new BehaviorSubject<T>(null);
    private _overrideValue = new BehaviorSubject<{ useOverride: boolean; value?: T }>({
        useOverride: false,
    });
    private _mergedValue = new BehaviorSubject<T>(null);
    private _queryParam: string;

    constructor(private _type: UserModifiableType<T>, initialSystemValue?: T) {
        if (initialSystemValue !== undefined) {
            this.setSystemValue(initialSystemValue);
        }
        this._getMergedValue().subscribe(this._mergedValue);
    }

    /**
     * Sets the default system value, that will be used when no user value is set.
     */
    setSystemValue(value: T): void {
        if (this._serialize(value) !== this._serialize(this._systemValue.value)) {
            this._systemValue.next(value);
        }
    }

    /**
     * Sets the user value, that will replace or be merged with the system value.
     */
    setUserValue(value: T): void {
        if (this._serialize(value) !== this._serialize(this._userValue.value)) {
            // console.log('setUserValue', { value, systemValue: this._systemValue.value });
            if (this._serialize(value) === this._serialize(this._systemValue.value)) {
                this.resetUserValue();
            } else {
                this._userValue.next(value);
            }
        }
    }

    resetUserValue(): void {
        if (this._userValue.value !== null) {
            // console.log('resetUserValue');
            this._userValue.next(null);
        }
    }

    getUserValue(): T {
        return this._userValue.value;
    }

    observeUserValue(): Observable<T> {
        return this._userValue.asObservable();
    }

    /**
     * Sets an override value that will supersede any user and system values.
     */
    setOverrideValue(value: T): void {
        if (
            !this._overrideValue.value.useOverride ||
            this._serialize(this._overrideValue.value.value) !== this._serialize(value)
        ) {
            this._overrideValue.next({ useOverride: true, value });
        }
    }

    unsetOverrideValue(): void {
        if (this._overrideValue.value.useOverride) {
            this._overrideValue.next({ useOverride: false });
        }
    }

    getValue(): T {
        return this._mergedValue.value;
    }

    observeValue(): Observable<T> {
        return this._mergedValue.asObservable();
    }

    getQueryParamEntry(value: T = this.getValue()): { [key: string]: string } {
        const serializedValue = this._serialize(value);
        if (serializedValue === this._serialize(this._systemValue.value)) {
            return {};
        } else {
            return { [this._queryParam]: serializedValue };
        }
    }

    registerQueryParameter(
        key: string,
        activatedRoute: ActivatedRoute,
        { replaceUrl = false } = {},
    ): void {
        if (this._queryParam) {
            console.warn(
                `Registered user value for query parameter ${key} ` +
                    `which is already registered for query parameter ${this._queryParam}`,
            );
        }
        this._queryParam = key;
        let currentParam: string;
        activatedRoute.queryParams
            .pipe(
                map((params) => params[key] as string),
                distinctUntilChanged(),
                filter((param) => param !== currentParam),
                tap((param) => (currentParam = param)),
                map((param) => this._deserialize(param)),
                // tap((queryParam) => console.log('queryParams', { key, queryParam })),
            )
            .subscribe((value) => this.setUserValue(value));
        this._userValue
            .pipe(
                map((value) => this._serialize(value)),
                filter((param) => param !== currentParam),
                tap((param) => (currentParam = param)),
            )
            .subscribe((param) => {
                UserModifiableValue.navigationScheduler.scheduleNavigation({
                    queryParams: { [key]: param },
                    replaceUrl,
                });
            });
    }

    registerSessionStorage(key: string): void {
        // Query the storage value only once on registration. We expect to be the only one accessing
        // this value.
        let storageValue = sessionStorage.getItem(key);
        if (notNull(storageValue)) {
            this.setUserValue(this._deserialize(storageValue));
        }
        this._userValue.pipe(map((value) => this._serialize(value))).subscribe((value) => {
            if (storageValue !== value) {
                storageValue = value;
                if (notNull(value)) {
                    sessionStorage.setItem(key, value);
                } else {
                    sessionStorage.removeItem(key);
                }
            }
        });
    }

    // registerProfilePreference(key: string, until: Observable<void>): void {
    //     let currentStorageValue: string;
    //     UserModifiableValue.preferences
    //         .observe(key)
    //         .pipe(
    //             filter((value: T) => this._serialize(value) !== currentStorageValue),
    //             tap((value: T) => (currentStorageValue = this._serialize(value))),
    //             takeUntil(until),
    //         )
    //         .subscribe((value: T) => this.setUserValue(value));
    //     this._userValue
    //         .pipe(filter((value: T) => this._serialize(value) !== currentStorageValue))
    //         .subscribe((userValue) => UserModifiableValue.preferences.set(key, userValue));
    // }

    registerFormControl(formControl: FormControl): void {
        this.observeValue()
            .pipe(
                map((value) => this._serialize(value)),
                filter((value) => value !== formControl.value),
            )
            .subscribe((value) => formControl.setValue(value));
        formControl.valueChanges
            .pipe(
                map((value) => this._deserialize(value)),
                // tap((value) => console.log('formControl', value)),
            )
            .subscribe((value) => this.setUserValue(value));
    }

    private _serialize(value: T | null): string | null {
        if (notNull(value)) {
            return this._type.serialize(value);
        } else {
            return null;
        }
    }

    private _deserialize(value: string | null): T | null {
        if (notNull(value)) {
            return this._type.deserialize(value);
        } else {
            return null;
        }
    }

    private _getMergedValue(): Observable<T> {
        return rxjs.combineLatest([this._systemValue, this._userValue, this._overrideValue]).pipe(
            map(([systemValue, userValue, overrideValue]) =>
                overrideValue.useOverride
                    ? overrideValue.value
                    : this._type.merge(systemValue, userValue),
            ),
            distinctUntilChanged(),
        );
    }
}

interface UserModifiableType<T> {
    null: T;
    merge: (defaultValue: T, userValue: T) => T;
    serialize: (value: T) => string;
    deserialize: (value: string) => T;
}

const userModifiableDictType: UserModifiableType<{ [key: string]: any }> = {
    null: {},
    merge: (defaultValue: { [key: string]: any }, userValue: { [key: string]: any }) => ({
        ...defaultValue,
        ...userValue,
    }),
    serialize: JSON.stringify,
    deserialize: JSON.parse,
};

class UserModifiableStringType<S extends string> implements UserModifiableType<S> {
    null: S = null;
    merge = (defaultValue: S, userValue: S) => userValue ?? defaultValue;
    serialize = (value: S) => value;
    deserialize = (value: string) => value as S;
}

interface Mapping<T> {
    toString: (value: T) => string;
    fromString: (value: string) => T;
}

class UserModifiableMappedType<T> implements UserModifiableType<T> {
    null: T = null;
    merge = (defaultValue: T, userValue: T) => userValue ?? defaultValue;
    serialize = this.mapping.toString;
    deserialize = this.mapping.fromString;

    constructor(private mapping: Mapping<T>) {}
}

const userModifiableBooleanType: UserModifiableType<boolean> = {
    null: false,
    merge: (defaultValue: boolean, userValue: boolean) => userValue ?? defaultValue,
    serialize: (value: boolean) => (value ? 'true' : 'false'),
    deserialize: (value: string) => value === 'true',
};
