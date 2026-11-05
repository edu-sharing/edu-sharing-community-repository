import { Component, computed, inject, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Ace, Node, Person, RestConstants } from 'ngx-edu-sharing-api';
import { EduSharingUiCommonModule, UserAvatarComponent } from 'ngx-edu-sharing-ui';
import { CollectionInfoSectionComponent } from '../collection-info-section/collection-info-section.component';
import { CollectionDetailsService } from '../../collection-details.service';

/** One person or group of the participants list. */
interface Participant {
    /** identifies the row in the list */
    key: string;
    /** what the avatar and the name are derived from: an invitation or the owner of the collection */
    source: Ace | Person;
    /** the name of the person or group, in the form the `authorityName` pipe expects */
    profile: unknown;
    isOwner: boolean;
    /** translation key of the role, the owner is labelled as such instead */
    roleKey: string;
}

/** roles from the one with the most rights to the one with the fewest */
const ROLES = [
    { permission: RestConstants.PERMISSION_COORDINATOR, key: 'WORKSPACE.SHARE.TYPE_COORDINATOR' },
    { permission: RestConstants.PERMISSION_COLLABORATOR, key: 'WORKSPACE.SHARE.TYPE_COWORKER' },
    { permission: RestConstants.ACCESS_CONSUMER, key: 'WORKSPACE.SHARE.TYPE_VIEWER' },
];
const UNKNOWN_ROLE_KEY = 'WORKSPACE.SHARE.TYPE_UNKNOWN';
/** authorities that are part of every permission list but are no participants */
const HIDDEN_AUTHORITY_NAMES = ['System'];

/**
 * The people and groups involved in a collection, one row each with the role they have. The
 * invitations are only known to those who may manage the permissions of the collection; everybody
 * else sees the owner only.
 */
@Component({
    selector: 'es-collection-info-participants',
    templateUrl: 'collection-info-participants.component.html',
    styleUrls: ['collection-info-participants.component.scss'],
    imports: [
        TranslateModule,
        EduSharingUiCommonModule,
        CollectionInfoSectionComponent,
        UserAvatarComponent,
    ],
})
export class CollectionInfoParticipantsComponent {
    private readonly details = inject(CollectionDetailsService);

    readonly collection = input.required<Node>();

    protected readonly participants = computed<Participant[]>(() => {
        const ownerName = this.details.ownerName();
        const invited = (this.details.uniquePermissions() ?? []).filter(
            (ace) =>
                ace.authority.authorityType !== 'EVERYONE' &&
                !HIDDEN_AUTHORITY_NAMES.includes(ace.authority.authorityName),
        );
        const rows: Participant[] = invited.map((ace) => {
            const isOwner = ace.authority.authorityName === ownerName;
            return {
                key: ace.authority.authorityName,
                source: ace,
                profile: ace.authority.authorityType === 'GROUP' ? ace.group : ace.user,
                isOwner,
                roleKey: this.roleKey(ace),
            };
        });
        if (this.collection()?.owner && !rows.some((row) => row.isOwner)) {
            rows.push({
                key: ownerName ?? 'owner',
                source: this.collection().owner,
                profile: this.collection().owner,
                isOwner: true,
                roleKey: '',
            });
        }
        return rows.sort((a, b) => this.rank(a) - this.rank(b));
    });

    /** the name that is shown for the owner when the creator was entered as free text */
    protected readonly authorFreetext = computed(
        () => this.collection()?.collection?.authorFreetext,
    );

    private roleKey(ace: Ace): string {
        const role = ROLES.find((r) => ace.permissions?.includes(r.permission));
        return role?.key ?? UNKNOWN_ROLE_KEY;
    }

    /** the owner comes first, then the roles by their rights */
    private rank(participant: Participant): number {
        if (participant.isOwner) {
            return -1;
        }
        const index = ROLES.findIndex((r) => r.key === participant.roleKey);
        return index === -1 ? ROLES.length : index;
    }
}
