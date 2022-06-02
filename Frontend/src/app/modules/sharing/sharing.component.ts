import { Component, ViewChild, HostListener, ElementRef } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { TranslationsService } from '../../translations/translations.service';
import { ListItem, RestSearchService } from '../../core-module/core.module';
import { RestNodeService } from '../../core-module/core.module';
import { RestConstants } from '../../core-module/core.module';
import { RestConnectorService } from '../../core-module/core.module';
import { Node, NodeList, LoginResult, SharingInfo, Person } from '../../core-module/core.module';
import { CustomOptions, OptionItem } from '../../core-ui-module/option-item';
import { TemporaryStorageService } from '../../core-module/core.module';
import { ConfigurationService } from '../../core-module/core.module';
import { UIConstants } from '../../core-module/ui/ui-constants';
import { RestMdsService } from '../../core-module/core.module';
import { RestHelper } from '../../core-module/core.module';
import { RestSharingService } from '../../core-module/core.module';
import { Toast } from '../../core-ui-module/toast';
import { ConfigurationHelper } from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';

@Component({
    selector: 'es-sharing',
    templateUrl: 'sharing.component.html',
    styleUrls: ['sharing.component.scss'],
})
export class SharingComponent {
    loading = true;
    loadingChildren = true;
    passwordInput: string;
    private params: Params;
    sharingInfo: SharingInfo;
    childs: Node[];
    columns: ListItem[] = [];
    sort = {
        sortBy: RestConstants.CM_NAME,
        sortAscending: true,
    };
    options: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private connector: RestConnectorService,
        private nodeService: RestNodeService,
        private sharingService: RestSharingService,
        private storage: TemporaryStorageService,
        private toast: Toast,
        private config: ConfigurationService,
        private translations: TranslationsService,
    ) {
        this.columns.push(new ListItem('NODE', RestConstants.CM_NAME));
        this.columns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        this.columns.push(new ListItem('NODE', RestConstants.SIZE));
        this.options.addOptions.push(
            new OptionItem('SHARING.DOWNLOAD', 'cloud_download', (node: Node) =>
                this.download(node),
            ),
        );
        this.translations.waitForInit().subscribe(() => {
            this.route.queryParams.subscribe((params) => {
                this.params = params;
                this.sharingService.getInfo(params.nodeId, params.token).subscribe(
                    (result) => {
                        this.loading = false;
                        this.sharingInfo = result;
                        if (result.expired) {
                            this.router.navigate([
                                UIConstants.ROUTER_PREFIX,
                                'messages',
                                'share_expired',
                            ]);
                            return;
                        }
                        this.loadChildren();
                    },
                    (error) => {
                        console.warn(error);
                        this.router.navigate([
                            UIConstants.ROUTER_PREFIX,
                            'messages',
                            'share_expired',
                        ]);
                        this.loading = false;
                    },
                );
            });
        });
    }
    validatePassword() {
        this.sharingService
            .getInfo(this.params.nodeId, this.params.token, this.passwordInput)
            .subscribe((result) => {
                if (!result.passwordMatches) {
                    this.toast.error(null, 'SHARING.ERROR_INVALID_PASSWORD');
                }
                this.sharingInfo = result;
                this.loadChildren();
            });
    }
    download(child: Node = null) {
        const node = this.params.nodeId;
        const token = this.params.token;
        let url =
            this.connector.getAbsoluteEndpointUrl() +
            '../share?mode=download&token=' +
            encodeURIComponent(token) +
            '&password=' +
            encodeURIComponent(this.passwordInput) +
            '&nodeId=' +
            encodeURIComponent(node);
        if (child == null && this.sharingInfo.node.isDirectory) {
            const ids = RestHelper.getNodeIds(this.childs).join(',');
            url += '&childIds=' + encodeURIComponent(ids);
        } else {
            if (child != null) {
                url += '&childIds=' + encodeURIComponent(child.ref.id);
            }
        }
        window.open(url);
    }
    changeSort(sort: any) {
        this.sort = sort;
        this.loadChildren();
    }
    private loadChildren() {
        if (this.sharingInfo.password && !this.sharingInfo.passwordMatches) return;
        this.loadingChildren = true;
        this.childs = [];
        const request = {
            count: RestConstants.COUNT_UNLIMITED,
            sortBy: [this.sort.sortBy],
            sortAscending: [this.sort.sortAscending],
            propertyFilter: [RestConstants.ALL],
        };
        this.sharingService
            .getChildren(this.params.nodeId, this.params.token, this.passwordInput, request)
            .subscribe((nodes) => {
                this.childs = nodes.nodes;
                this.loadingChildren = false;
            });
    }
    inviterIsAuthor() {
        return Helper.objectEquals(this.sharingInfo.invitedBy, this.sharingInfo.node.createdBy);
    }
    getPersonName(person: Person) {
        return ConfigurationHelper.getPersonWithConfigDisplayName(person, this.config);
    }

    childCount() {
        if (this.sharingInfo.node.type === RestConstants.CCM_TYPE_IO) {
            try {
                return (
                    parseInt(
                        this.sharingInfo.node.properties[
                            RestConstants.VIRTUAL_PROP_CHILDOBJECTCOUNT
                        ]?.[0],
                        10,
                    ) || 0
                );
            } catch (e) {}
        }
        return 0;
    }
}
