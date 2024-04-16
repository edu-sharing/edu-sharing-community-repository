import { APP_INITIALIZER, Injector } from '@angular/core';
import { Decorator } from '@storybook/angular';

// Adapted from https://jgelin.medium.com/inject-angular-services-in-storybook-7-c26b7f5a41e5
export function injectInjectorToProps<TArgs = unknown>(): Decorator<TArgs> {
    return (storyFn) => {
        const story = storyFn();

        if (!story.applicationConfig) {
            story.applicationConfig = { providers: [] };
        }

        story.applicationConfig.providers.push({
            provide: APP_INITIALIZER,
            useFactory: (injector: Injector): void => {
                if (!story.props) {
                    story.props = { injector };
                }
                Object.assign(story.props, { injector });
            },
            deps: [Injector],
        });

        return story;
    };
}
