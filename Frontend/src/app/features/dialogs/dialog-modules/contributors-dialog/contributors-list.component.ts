import { Component, computed, input, output } from '@angular/core';
import {
    ContributorBucket,
    ContributorEntry,
    rolesOf,
    visibleEntries,
} from './contributor-bulk-model';

interface RoleGroup {
    role: string;
    entries: ContributorEntry[];
}

/**
 * Renders the contributors of one bucket (lifecycle / metadata), grouped by role.
 *
 * Purely presentational - it holds no dialog reference and no services, so it can be rendered in
 * storybook and reasoned about on its own. All mutations go through the outputs.
 *
 * In bulk mode a contributor that is only present on some of the selected nodes is shown as an
 * indeterminate chip, built exactly like the chips of the mds multivalue tree: label plus remove,
 * hatched background, no extra affordances. Removing it drops it from every node; applying it to
 * every node happens by opening it, which is also where the user confirms the unified data.
 */
@Component({
    selector: 'es-contributors-list',
    templateUrl: './contributors-list.component.html',
    styleUrls: ['./contributors-list.component.scss'],
    standalone: false,
})
export class ContributorsListComponent {
    readonly bucket = input.required<ContributorBucket>();
    readonly entries = input.required<ContributorEntry[]>();
    /** number of selected nodes; > 1 enables the bulk affordances */
    readonly nodeCount = input(1);

    readonly remove = output<ContributorEntry>();
    readonly edit = output<ContributorEntry>();
    readonly add = output<void>();

    readonly isBulk = computed(() => this.nodeCount() > 1);

    readonly heading = computed(() =>
        this.bucket() === 'lifecycle'
            ? 'WORKSPACE.CONTRIBUTOR.LIFECYCLECONTRIBUTOR'
            : 'WORKSPACE.CONTRIBUTOR.METADATACONTRIBUTOR',
    );

    readonly groups = computed<RoleGroup[]>(() => {
        const entries = this.entries();
        return rolesOf(this.bucket())
            .map((role) => ({ role, entries: visibleEntries(entries, this.bucket(), role) }))
            .filter((group) => group.entries.length > 0);
    });

    /** ids are prefixed by bucket, since both buckets are rendered next to each other */
    readonly headingId = computed(() => `contributors-${this.bucket()}-heading`);

    roleId(role: string): string {
        return `contributors-${this.bucket()}-role-${role}`;
    }

    roleLabel(role: string): string {
        return 'WORKSPACE.CONTRIBUTOR.TYPE.' + role.toUpperCase();
    }
}
