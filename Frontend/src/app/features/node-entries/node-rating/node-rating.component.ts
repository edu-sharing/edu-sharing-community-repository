import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import {
    ConfigurationService,
    Node,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestRatingService,
} from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';

@Component({
    selector: 'es-node-rating',
    templateUrl: 'node-rating.component.html',
    styleUrls: ['node-rating.component.scss'],
})
export class NodeRatingComponent<T extends Node> implements OnInit {
    @Input() node: T;
    mode: RatingMode;
    hasPermission: boolean;
    hoverStar: number;
    constructor(
        public connector: RestConnectorService,
        public toast: Toast,
        public configService: ConfigurationService,
        public changeDetectorRef: ChangeDetectorRef,
        public ratingService: RestRatingService,
    ) {}

    async ngOnInit() {
        this.mode = await this.configService.get('rating.mode', 'none').toPromise();
        this.hasPermission = await this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_RATE_READ)
            .toPromise();
    }

    async toogleLike() {
        const name = RestHelper.getTitle(this.node);
        if (this.node.rating?.user) {
            try {
                await this.ratingService.deleteNodeRating(this.node.ref.id).toPromise();
                this.toast.toast('RATING.TOAST.LIKE_REMOVED', { name });
                this.node.rating.user = 0;
                this.node.rating.overall.count--;
            } catch (e) {
                this.toast.error(e);
            }
        } else {
            try {
                await this.ratingService.updateNodeRating(this.node.ref.id, 5).toPromise();
                this.toast.toast('RATING.TOAST.LIKED', { name });
                this.node.rating.user = 5;
                this.node.rating.overall.count++;
            } catch (e) {
                this.toast.error(e);
            }
        }
        this.changeDetectorRef.detectChanges();
    }

    getPrimaryRating() {
        if (!this.node.rating) {
            return 0;
        }
        if (this.node.rating.user) {
            return this.node.rating.user;
        }
        return this.node.rating.overall.sum / this.node.rating.overall.count;
    }

    async setRating(rating: number) {
        const name = RestHelper.getTitle(this.node);
        try {
            await this.ratingService.updateNodeRating(this.node.ref.id, rating).toPromise();
            this.toast.toast('RATING.TOAST.RATED', { name, rating });
            this.node.rating.overall.count += this.node.rating.user ? 0 : 1;
            this.node.rating.user = rating;
            this.changeDetectorRef.detectChanges();
        } catch (e) {
            this.toast.error(e);
        }
    }

    async deleteRating() {
        const name = RestHelper.getTitle(this.node);
        try {
            await this.ratingService.deleteNodeRating(this.node.ref.id).toPromise();
            this.toast.toast('RATING.TOAST.RATING_REMOVED', { name });
            this.node.rating.overall.count--;
            this.node.rating.overall.sum -= this.node.rating.user;
            this.node.rating.user = 0;
            this.changeDetectorRef.detectChanges();
        } catch (e) {
            this.toast.error(e);
        }
    }
}
export type RatingMode = 'none' | 'likes' | 'stars';
