import { Injectable } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
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
    constructor(navigationScheduler: NavigationScheduler) {
        UserModifiableValue.navigationScheduler = navigationScheduler;
    }

    createDict(systemValue?: { [key: string]: any }): UserModifiableValue<{ [key: string]: any }> {
        return new UserModifiableValue(userModifiableDictType, systemValue);
    }

    createString(systemValue?: string): UserModifiableValue<string> {
        return new UserModifiableValue(userModifiableStringType, systemValue);
    }

    // createMapped<T>(mapping: Mapping<T>, systemValue?: T): UserModifiableValue<T> {
    //     return new UserModifiableValue<T>(new UserModifiableMappedType(mapping), systemValue);
    // }

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
class UserModifiableValue<T> {
    static navigationScheduler: NavigationScheduler;

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

    private systemValue = new BehaviorSubject<T>(this.type.null);
    private userValue = new BehaviorSubject<T>(null);
    private overrideValue = new BehaviorSubject<{ useOverride: boolean; value?: T }>({
        useOverride: false,
    });
    private mergedValue = new BehaviorSubject<T>(null);

    constructor(private type: UserModifiableType<T>, initialSystemValue?: T) {
        if (initialSystemValue) {
            this.setSystemValue(initialSystemValue);
        }
        this.getMergedValue().subscribe(this.mergedValue);
    }

    /**
     * Sets the default system value, that will be used when no user value is set.
     */
    setSystemValue(value: T): void {
        if (this.type.serialize(value) !== this.type.serialize(this.systemValue.value)) {
            this.systemValue.next(value);
        }
    }

    /**
     * Sets the user value, that will replace or be merged with the system value.
     */
    setUserValue(value: T): void {
        if (this.type.serialize(value) !== this.type.serialize(this.userValue.value)) {
            // console.log('setUserValue', value);
            if (this.type.serialize(value) === this.type.serialize(this.systemValue.value)) {
                this.resetUserValue();
            } else {
                this.userValue.next(value);
            }
        }
    }

    resetUserValue(): void {
        if (this.userValue.value !== null) {
            // console.log('resetUserValue');
            this.userValue.next(null);
        }
    }

    getUserValue(): T {
        return this.userValue.value;
    }

    observeUserValue(): Observable<T> {
        return this.userValue.asObservable();
    }

    /**
     * Sets an override value that will supersede any user and system values.
     */
    setOverrideValue(value: T): void {
        if (
            !this.overrideValue.value.useOverride ||
            this.type.serialize(this.overrideValue.value.value) !== this.type.serialize(value)
        ) {
            this.overrideValue.next({ useOverride: true, value });
        }
    }

    unsetOverrideValue(): void {
        if (this.overrideValue.value.useOverride) {
            this.overrideValue.next({ useOverride: false });
        }
    }

    getValue(): T {
        return this.mergedValue.value;
    }

    observeValue(): Observable<T> {
        return this.mergedValue.asObservable();
    }

    registerQueryParameter(key: string, activatedRoute: ActivatedRoute): void {
        let currentParam: string;
        activatedRoute.queryParams
            .pipe(
                map((params) => params[key] as string),
                distinctUntilChanged(),
                filter((param) => param !== currentParam),
                tap((param) => (currentParam = param)),
                map((param) => (param ? this.type.deserialize(param) : null)),
                // tap((queryParam) => console.log('queryParams', { key, queryParam })),
            )
            .subscribe((value) => this.userValue.next(value));
        this.userValue
            .pipe(
                map((value) => (value ? this.type.serialize(value) : null)),
                filter((param) => param !== currentParam),
                tap((param) => (currentParam = param)),
            )
            .subscribe((param) => {
                UserModifiableValue.navigationScheduler.scheduleNavigation({
                    queryParams: { [key]: param },
                });
            });
    }

    registerFormControl(formControl: FormControl): void {
        this.observeValue()
            .pipe(
                map((value) => this.type.serialize(value)),
                filter((value) => value !== formControl.value),
            )
            .subscribe((value) => formControl.setValue(value));
        formControl.valueChanges
            .pipe(
                map((value) => this.type.deserialize(value)),
                // tap((value) => console.log('formControl', value)),
            )
            .subscribe((value) => this.setUserValue(value));
    }

    private getMergedValue(): Observable<T> {
        return rxjs.combineLatest([this.systemValue, this.userValue, this.overrideValue]).pipe(
            map(([systemValue, userValue, overrideValue]) =>
                overrideValue.useOverride
                    ? overrideValue.value
                    : this.type.merge(systemValue, userValue),
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

const userModifiableStringType: UserModifiableType<string> = {
    null: null,
    merge: (defaultValue: string, userValue: string) => userValue ?? defaultValue,
    serialize: (value: string) => value,
    deserialize: (value: string) => value,
};

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
