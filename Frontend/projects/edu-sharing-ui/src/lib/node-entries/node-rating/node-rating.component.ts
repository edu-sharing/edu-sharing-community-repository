import { ChangeDetectorRef, Component, Input, OnInit, inject } from '@angular/core';
import {
    AuthenticationService,
    ConfigService,
    NetworkService,
    Node,
    RatingV1Service,
    RestConstants,
} from 'ngx-edu-sharing-api';
import { Toast } from '../../services/abstract/toast.service';
import { take } from 'rxjs/operators';
import { RestHelper } from '../../util/rest-helper';

@Component({
    selector: 'es-node-rating',
    templateUrl: 'node-rating.component.html',
    styleUrls: ['node-rating.component.scss'],
    standalone: false,
})
export class NodeRatingComponent<T extends Node> implements OnInit {
    toast = inject(Toast);
    configService = inject(ConfigService);
    private networkApi = inject(NetworkService);
    authenticationService = inject(AuthenticationService);
    changeDetectorRef = inject(ChangeDetectorRef);
    ratingService = inject(RatingV1Service);

    @Input() node: T;
    mode: RatingMode;
    hasPermission: boolean;
    hoverStar: number;

    async ngOnInit() {
        await this.configService.observeConfig().pipe(take(1)).toPromise();
        this.mode = this.configService.instant('rating.mode', 'none');
        this.hasPermission = await this.authenticationService.hasToolpermission(
            RestConstants.TOOLPERMISSION_RATE_READ,
        );
    }

    async toogleLike() {
        const name = RestHelper.getTitle(this.node);
        if (this.node.rating?.user) {
            try {
                await this.ratingService
                    .deleteRating({
                        repository: this.node.ref.repo,
                        node: this.node.ref.id,
                    })
                    .toPromise();
                this.toast.toast('RATING.TOAST.LIKE_REMOVED', { name });
                this.node.rating.user = 0;
                this.node.rating.overall.count--;
            } catch (e) {
                this.toast.error(e);
            }
        } else {
            try {
                await this.ratingService
                    .addOrUpdateRating({
                        repository: this.node.ref.repo,
                        node: this.node.ref.id,
                        rating: 5,
                        body: null,
                    })
                    .toPromise();
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
            await this.ratingService
                .addOrUpdateRating({
                    repository: this.node.ref.repo,
                    node: this.node.ref.id,
                    rating,
                    body: null,
                })
                .toPromise();
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
            await this.ratingService
                .deleteRating({
                    repository: this.node.ref.repo,
                    node: this.node.ref.id,
                })
                .toPromise();
            this.toast.toast('RATING.TOAST.RATING_REMOVED', { name });
            this.node.rating.overall.count--;
            this.node.rating.overall.sum -= this.node.rating.user;
            this.node.rating.user = 0;
            this.changeDetectorRef.detectChanges();
        } catch (e) {
            this.toast.error(e);
        }
    }

    isFromHomeRepo(node: Node) {
        return this.networkApi.isFromHomeRepository(node);
    }
}
export type RatingMode = 'none' | 'likes' | 'stars';
