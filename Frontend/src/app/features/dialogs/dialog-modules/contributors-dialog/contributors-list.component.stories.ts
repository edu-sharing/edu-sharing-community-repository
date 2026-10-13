import { applicationConfig, type Meta, moduleMetadata, type StoryObj } from '@storybook/angular';
import { expect, fn, userEvent, waitFor } from 'storybook/test';
import { SharedModule } from '../../../../shared/shared.module';
import { mdsStorybookProviders } from '../../../mds/mds-editor/storybook-utils';
import { ContributorsListComponent } from './contributors-list.component';
import {
    conflictingNodes,
    entriesOf,
    mixedNodes,
    noOverlapNodes,
    nodeWith,
    organization,
    person,
    singleNode,
} from './contributor-fixtures';

/**
 * The contributor list of one bucket. It is the presentational half of the contributors dialog and
 * carries no dialog reference, so it can be rendered on its own here.
 *
 * Use the theme switcher in the toolbar to check the indeterminate chips in dark mode, and the
 * a11y panel to verify that the state is announced and not only hatched.
 */
const meta: Meta<ContributorsListComponent> = {
    title: 'Dialogs/Contributors',
    component: ContributorsListComponent,
    decorators: [
        moduleMetadata({
            imports: [SharedModule],
        }),
        applicationConfig({
            providers: mdsStorybookProviders,
        }),
    ],
    argTypes: {
        bucket: { control: 'inline-radio', options: ['lifecycle', 'metadata'] },
        nodeCount: { control: { type: 'number', min: 1 } },
    },
    args: {
        bucket: 'lifecycle',
    },
    tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<ContributorsListComponent>;

/** A single node: every contributor applies to it, so no bulk affordances are shown. */
export const SingleNode: Story = {
    args: {
        entries: entriesOf(singleNode()),
        nodeCount: 1,
    },
};

/** Three nodes without a single contributor in common - everything is indeterminate. */
export const BulkNoOverlap: Story = {
    args: {
        entries: entriesOf(noOverlapNodes()),
        nodeCount: 3,
    },
};

/** The common case: one shared contributor plus one of each node's own. */
export const BulkMixed: Story = {
    args: {
        entries: entriesOf(mixedNodes()),
        nodeCount: 2,
        remove: fn(),
    },
    // Query by class rather than by label: the storybook translate mock loads the real german
    // translations asynchronously, so the visible text is not stable at this point.
    play: async ({ args, canvasElement }) => {
        // Ada is on both nodes, Grace and Alan on one each
        const indeterminate = canvasElement.querySelectorAll('mat-chip-row.indeterminate');
        expect(indeterminate.length).toBe(2);
        // a hatched chip carries nothing but its label and one keyboard-focusable remove button,
        // like an individual value of the mds multivalue tree
        expect(indeterminate[0].querySelectorAll('button[matchipremove]').length).toBe(1);
        await userEvent.click(indeterminate[0].querySelector('[matchipremove]'));
        await waitFor(() => expect(args.remove).toHaveBeenCalled());
    },
};

/** Same contributor (matched by orcid) but the nodes disagree on mail and contribute date. */
export const BulkWithConflict: Story = {
    args: {
        entries: entriesOf(conflictingNodes()),
        nodeCount: 2,
    },
};

/** Nothing set yet - only the add button. */
export const Empty: Story = {
    args: {
        entries: [],
        nodeCount: 3,
    },
};

/** Layout stress test: many roles, long names and organizations. */
export const ManyRolesLongNames: Story = {
    args: {
        entries: entriesOf([
            nodeWith('node-1', {
                'lifecycle:author': [
                    person({ givenname: 'Maximiliane', surname: 'Schmidt-Hohenzollern' }),
                    person({ givenname: 'Jean-Baptiste', surname: 'de la Fontaine-Rousseau' }),
                ],
                'lifecycle:publisher': [
                    organization('Landesinstitut für Schulentwicklung und Medienbildung'),
                ],
                'lifecycle:editor': [person({ givenname: 'Ada', surname: 'Lovelace' })],
                'lifecycle:content_provider': [
                    organization('Zentrum für Digitale Bildung und Medienkompetenz gGmbH'),
                ],
            }),
            nodeWith('node-2', {
                'lifecycle:author': [
                    person({ givenname: 'Maximiliane', surname: 'Schmidt-Hohenzollern' }),
                ],
                'lifecycle:sprecher': [person({ givenname: 'Grace', surname: 'Hopper' })],
            }),
        ]),
        nodeCount: 2,
    },
};
