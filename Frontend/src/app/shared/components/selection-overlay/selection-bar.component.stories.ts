import { Component, Input } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { ApiRequestConfiguration, AuthenticationService, Node } from 'ngx-edu-sharing-api';
import { Helper, NodeEntriesDisplayType } from 'ngx-edu-sharing-ui';
import { AuthenticationServiceMock } from '../../../../../projects/edu-sharing-ui/src/lib/node-entries/node-entries-card/node-entries-card.component.stories';
import {
    DefaultColumns,
    DummyNode,
    mdsStorybookProviders,
} from '../../../features/mds/mds-editor/storybook-utils';
import { SharedModule } from '../../shared.module';

/** `count` distinct copies of `DummyNode`, numbered from 1. */
function makeNodes(count: number): Node[] {
    return Array.from({ length: count }, (_, i) => {
        const node: Node = Helper.deepCopy(DummyNode);
        node.ref.id = 'id_' + (i + 1);
        node.title = `${node.title} ${i + 1}`;
        return node;
    });
}

type SelectionBarMode = 'selection' | 'clipboard';

interface SelectionBarStoryArgs {
    /** Number of selected (or clipboard) nodes at start. */
    count: number;
    /** Display type of the page's list; the panel shows a table or small cards to match. */
    displayType: NodeEntriesDisplayType;
    /** Whether the bar shows the selection or the clipboard. */
    mode: SelectionBarMode;
    /** Whether the panel is open at start. */
    open: boolean;
}

/**
 * Card like the node pages (search, workspace, collections, …): a list with checkboxes that
 * scrolls internally, the selection bar pinned below it with projected actions, and an element
 * after the card, so the tab order through toggle, panel and actions can be followed.
 */
@Component({
    selector: 'es-selection-bar-story-host',
    template: `
        <div class="card">
            <ul class="list">
                @for (node of allNodes; track node.ref.id) {
                <li>
                    <input
                        type="checkbox"
                        [checked]="nodes.includes(node)"
                        (change)="toggle(node)"
                        [attr.aria-label]="'SELECT' | translate : { element: node.title }"
                    />
                    {{ node.title }}
                </li>
                }
            </ul>
            <es-selection-bar
                [class.visible]="nodes.length"
                [nodes]="nodes"
                [columns]="columns"
                [displayType]="displayType"
                [countLabel]="mode === 'clipboard' ? 'SEARCH.N_IN_CLIPBOARD' : 'SEARCH.N_SELECTED'"
                [clearLabel]="
                    mode === 'clipboard' ? 'SEARCH.CLEAR_CLIPBOARD' : 'SEARCH.CLEAR_SELECTION'
                "
                [panelLabel]="
                    mode === 'clipboard' ? 'SEARCH.CLIPBOARD_PANEL' : 'SEARCH.SELECTION_PANEL'
                "
                [(open)]="open"
                (clear)="nodes = []"
                (deselect)="toggle($event)"
            >
                <div class="story-actions">
                    <button mat-stroked-button color="primary" type="button">Einsortieren</button>
                    <button mat-stroked-button color="primary" type="button">Herunterladen</button>
                </div>
            </es-selection-bar>
        </div>
        <button mat-button type="button" (click)="reset()">Auswahl wiederherstellen</button>
    `,
    styles: [
        `
            :host {
                display: flex;
                flex-direction: column;
                gap: 16px;
                height: 600px;
                padding: 16px;
                box-sizing: border-box;
                background-color: var(--backgroundColor);
                color: var(--textMain);
            }
            .card {
                display: flex;
                flex-direction: column;
                flex: 1;
                min-height: 0;
                overflow: hidden;
                border-radius: var(--borderRadiusCards);
                background-color: var(--roundedContainerBackground);
            }
            .list {
                flex: 1 1 0;
                min-height: 0;
                overflow-y: auto;
                margin: 0;
                padding: 16px 25px;
                list-style: none;
            }
            .list li {
                padding: 6px 0;
            }
            .story-actions {
                display: flex;
                gap: 10px;
            }
        `,
    ],
    standalone: false,
})
class SelectionBarStoryHostComponent {
    readonly allNodes = makeNodes(12);
    readonly columns = DefaultColumns;
    nodes: Node[] = [];
    displayType = NodeEntriesDisplayType.SmallGrid;
    mode: SelectionBarMode = 'selection';
    open = false;
    private initialCount = 0;

    @Input() set args(args: SelectionBarStoryArgs) {
        this.initialCount = args.count;
        this.displayType = args.displayType;
        this.mode = args.mode;
        this.open = args.open;
        this.reset();
    }

    toggle(node: Node): void {
        this.nodes = this.nodes.includes(node)
            ? this.nodes.filter((n) => n !== node)
            : [...this.nodes, node];
    }

    reset(): void {
        this.nodes = this.allNodes.slice(0, this.initialCount);
    }
}

const meta: Meta<SelectionBarStoryArgs> = {
    title: 'Shared/Selection Bar',
    component: SelectionBarStoryHostComponent,
    decorators: [
        moduleMetadata({
            declarations: [SelectionBarStoryHostComponent],
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: [
                ...mdsStorybookProviders,
                provideAnimations(),
                { provide: ApiRequestConfiguration, useValue: {} },
                { provide: AuthenticationService, useClass: AuthenticationServiceMock },
            ],
        }),
    ],
    argTypes: {
        count: { control: { type: 'range', min: 0, max: 12 } },
        displayType: {
            control: 'select',
            options: [NodeEntriesDisplayType.SmallGrid, NodeEntriesDisplayType.Table],
            labels: {
                [NodeEntriesDisplayType.SmallGrid]: 'Kacheln',
                [NodeEntriesDisplayType.Table]: 'Tabelle',
            },
        },
        mode: { control: 'inline-radio', options: ['selection', 'clipboard'] },
        open: { control: 'boolean' },
    },
    args: {
        count: 2,
        displayType: NodeEntriesDisplayType.SmallGrid,
        mode: 'selection',
        open: false,
    },
    tags: ['autodocs'],
    render: (args) => ({
        props: { args },
        template: `<es-selection-bar-story-host [args]="args"></es-selection-bar-story-host>`,
    }),
};

export default meta;
type Story = StoryObj<SelectionBarStoryArgs>;

/** Closed bar with two selected nodes; the toggle opens the panel above the bar. */
export const Default: Story = {};

/** Open panel with small cards, as for card views of the node list. */
export const OpenCards: Story = {
    args: { open: true },
};

/** Open panel as a table, as for list views; many nodes scroll inside the panel. */
export const OpenTable: Story = {
    args: { open: true, count: 8, displayType: NodeEntriesDisplayType.Table },
};

/** Clipboard mode, as shown in the workspace and collections without a selection. */
export const Clipboard: Story = {
    args: { open: true, mode: 'clipboard' },
};
