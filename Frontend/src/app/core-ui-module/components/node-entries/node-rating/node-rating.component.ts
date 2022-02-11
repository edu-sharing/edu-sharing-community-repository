import {
    ApplicationRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {NodeEntriesService} from '../../../node-entries.service';
import {Node} from '../../../../core-module/rest/data-object';
import {NodeHelperService} from '../../../node-helper.service';
import {ColorHelper, PreferredColor} from '../../../../core-module/ui/color-helper';
import {OptionItem, Target} from '../../../option-item';
import {DropdownComponent} from '../../dropdown/dropdown.component';
import {MatMenuTrigger} from '@angular/material/menu';
import {ClickSource, InteractionType} from '../../node-entries-wrapper/entries-model';
import { Toast } from 'src/app/core-ui-module/toast';
import {ConfigurationService} from '../../../../core-module/rest/services/configuration.service';
import {RestConnectorService} from '../../../../core-module/rest/services/rest-connector.service';
import {RestConstants} from '../../../../core-module/rest/rest-constants';
import {RestRatingService} from '../../../../core-module/rest/services/rest-rating.service';
import {RestHelper} from '../../../../core-module/rest/rest-helper';

@Component({
    selector: 'es-node-rating',
    templateUrl: 'node-rating.component.html',
    styleUrls: ['node-rating.component.scss']
})
export class NodeRatingComponent<T extends Node> implements OnInit {
    @Input() node: T;
    mode: RatingMode;
    hasPermission: boolean;
    constructor(
        public connector: RestConnectorService,
        public toast: Toast,
        public configService: ConfigurationService,
        public ratingService: RestRatingService,
    ) {
    }


    async ngOnInit() {
        this.mode = (await this.configService.get('rating.mode', 'none').toPromise());
        this.hasPermission = await this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_RATE_READ).toPromise();
    }

    async toogleLike() {
        const name = RestHelper.getTitle(this.node);
        if (this.node.rating.user) {
            try {
                await this.ratingService.deleteNodeRating(this.node.ref.id).toPromise();
                this.toast.toast('RATING.TOAST.LIKE_REMOVED', {name});
                this.node.rating.user = 0;
                this.node.rating.overall.count--;
            } catch (e) {
                this.toast.error(e);
            }
        } else {
            try {
                await this.ratingService.updateNodeRating(this.node.ref.id, 5).toPromise();
                this.toast.toast('RATING.TOAST.LIKED', {name});
                this.node.rating.user = 5;
                this.node.rating.overall.count++;
            } catch (e) {
                this.toast.error(e);
            }
        }
    }
}
export type RatingMode = 'none' | 'likes' | 'stars';
