import { signal } from '@angular/core';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { DEFAULT } from 'ngx-edu-sharing-api';
import { VCard } from 'ngx-edu-sharing-ui';
import { expect, userEvent, waitFor } from 'storybook/test';
import { SharedModule } from '../../../../../shared/shared.module';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { MdsModule } from '../../../mds.module';
import { MdsEditorWrapperComponent } from '../../mds-editor-wrapper/mds-editor-wrapper.component';
import { mdsStorybookProviders } from '../../storybook-utils';
import { Values } from '../../../types/types';
import {
    furtherAuthorsNodes,
    mixedAuthorNodes,
    sameFreetextNodes,
    singleAuthorNode,
} from './author-fixtures';

type StoryArgs = Partial<MdsEditorWrapperComponent>;

const FREETEXT = RestConstants.CCM_PROP_AUTHOR_FREETEXT;
const AUTHOR = RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR;

/**
 * The native author widget needs the editor around it, so the stories render the editor wrapper
 * with a view that contains only the author. The button below it shows the values saved per node.
 */
const meta: Meta<StoryArgs> = {
    title: 'Mds/Widget/Author',
    component: MdsEditorWrapperComponent,
    decorators: [
        moduleMetadata({
            imports: [MdsModule, SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    render: (args) => ({
        props: {
            ...args,
            values: signal(''),
            collect: async function (
                this: { values: ReturnType<typeof signal<string>> },
                mds: MdsEditorWrapperComponent,
            ) {
                const values: Values[] = [];
                for (const node of mds.mdsEditorInstance.nodes$.value) {
                    values.push(await mds.mdsEditorInstance.getValues(node, false));
                }
                this.values.set(JSON.stringify(values, null, 2));
            },
        },
        template: `
      <es-mds-editor-wrapper #mds
        [embedded]="embedded"
        [setId]="setId"
        [groupId]="groupId"
        [editorMode]="editorMode"
        [nodes]="nodes"
      ></es-mds-editor-wrapper>
      <button mat-flat-button color="primary" id="collect" (click)="collect(mds)">
        Test: Show values per node
      </button>
      <pre id="values">{{ values() }}</pre>
    `,
    }),
    args: {
        embedded: true,
        setId: DEFAULT,
        groupId: 'io_author_only',
        editorMode: 'nodes',
    },
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<StoryArgs>;

function switchOf(canvasElement: HTMLElement): HTMLElement {
    return canvasElement.querySelector('mat-slide-toggle button');
}

function freetextOf(canvasElement: HTMLElement): HTMLTextAreaElement {
    return canvasElement.querySelector('textarea#authorFreetext');
}

async function waitForWidget(canvasElement: HTMLElement): Promise<void> {
    await waitFor(() => expect(freetextOf(canvasElement)).not.toBeNull(), { timeout: 5000 });
}

async function collect(canvasElement: HTMLElement): Promise<Values[]> {
    await userEvent.click(canvasElement.querySelector('#collect'));
    const output = canvasElement.querySelector('#values');
    await waitFor(() => expect(output.textContent.trim()).not.toBe(''), { timeout: 5000 });
    return JSON.parse(output.textContent);
}

/** A single node: no switch, fields editable. */
export const SingleNode: Story = {
    args: { nodes: singleAuthorNode() },
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        expect(switchOf(canvasElement)).toBeNull();
        expect(freetextOf(canvasElement).disabled).toBe(false);
        expect(freetextOf(canvasElement).value).toBe('Free text');
    },
};

/** Same free text: shown but locked until the switch is on. */
export const BulkSameValues: Story = {
    args: { nodes: sameFreetextNodes() },
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        expect(switchOf(canvasElement)).not.toBeNull();
        expect(freetextOf(canvasElement).value).toBe('Shared author text');
        expect(freetextOf(canvasElement).disabled).toBe(true);
        await userEvent.click(switchOf(canvasElement));
        await waitFor(() => expect(freetextOf(canvasElement).disabled).toBe(false));
    },
};

/** Differing values: the field is empty and says so until edited. */
export const BulkMixedValues: Story = {
    args: { nodes: mixedAuthorNodes() },
    // checks the attribute, as the translations load asynchronously
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        expect(freetextOf(canvasElement).value).toBe('');
        expect(freetextOf(canvasElement).disabled).toBe(true);
        expect(freetextOf(canvasElement).placeholder).not.toBe('');
        await userEvent.click(switchOf(canvasElement));
        await waitFor(() => expect(freetextOf(canvasElement).disabled).toBe(false));
        await userEvent.type(freetextOf(canvasElement), 'x');
        expect(freetextOf(canvasElement).placeholder).toBe('');
    },
};

/** Only the free text changes, the person authors stay. */
export const BulkOnlyFreetextChanged: Story = {
    args: { nodes: mixedAuthorNodes() },
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        await userEvent.click(switchOf(canvasElement));
        await waitFor(() => expect(freetextOf(canvasElement).disabled).toBe(false));
        await userEvent.type(freetextOf(canvasElement), 'New text');
        const values = await collect(canvasElement);
        expect(values.length).toBe(2);
        values.forEach((v) => {
            expect(v[FREETEXT]).toEqual(['New text']);
            expect(AUTHOR in v).toBe(false);
        });
    },
};

/** Only the first person author is replaced, the others stay. */
export const BulkOnlyMainAuthorChanged: Story = {
    args: { nodes: furtherAuthorsNodes() },
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        await userEvent.click(switchOf(canvasElement));
        const tabs = canvasElement.querySelectorAll<HTMLElement>('[role="tab"]');
        await userEvent.click(tabs[1]);
        await waitFor(() => expect(canvasElement.querySelector('.givenname input')).not.toBeNull());
        await userEvent.type(canvasElement.querySelector('.givenname input'), 'Edsger');
        await userEvent.type(canvasElement.querySelector('.surname input'), 'Dijkstra');
        const values = await collect(canvasElement);
        values.forEach((v) => expect(FREETEXT in v).toBe(false));
        expect(values[0][AUTHOR].length).toBe(2);
        expect(new VCard(values[0][AUTHOR][0]).surname).toBe('Dijkstra');
        expect(new VCard(values[0][AUTHOR][1]).surname).toBe('Hopper');
        expect(values[1][AUTHOR].length).toBe(1);
        expect(new VCard(values[1][AUTHOR][0]).surname).toBe('Dijkstra');
    },
};

/** Switching off again discards the edit. */
export const BulkSwitchedOffWritesNothing: Story = {
    args: { nodes: mixedAuthorNodes() },
    play: async ({ canvasElement }) => {
        await waitForWidget(canvasElement);
        await userEvent.click(switchOf(canvasElement));
        await waitFor(() => expect(freetextOf(canvasElement).disabled).toBe(false));
        await userEvent.type(freetextOf(canvasElement), 'New text');
        await userEvent.click(switchOf(canvasElement));
        await waitFor(() => expect(freetextOf(canvasElement).disabled).toBe(true));
        const values = await collect(canvasElement);
        values.forEach((v) => {
            expect(FREETEXT in v).toBe(false);
            expect(AUTHOR in v).toBe(false);
        });
    },
};
