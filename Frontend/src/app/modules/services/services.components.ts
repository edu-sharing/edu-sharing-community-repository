
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {DomSanitizer, SafeResourceUrl, Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestNetworkService} from "../../common/rest/services/rest-network.service";
import {Toast} from "../../common/ui/toast";
import {Observable} from "rxjs/Rx";
import {AccessScope, Application, LoginResult, Service} from "../../common/rest/data-object";
import {Helper} from "../../common/helper";
import {RestHelper} from "../../common/rest/rest-helper";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestConstants} from "../../common/rest/rest-constants";
import {HttpClient} from '@angular/common/http';


@Component({
    selector: 'app-services',
    templateUrl: 'services.component.html',
    styleUrls: ['services.component.scss'],
})
export class ServicesComponent {

    serviceUrl:string;
    registeredServices:Service[] = [];
    stats: any = {};
    loading:number = 0;
    interfaces = ["Search","Sitemap","Statistics","OAI","Generic_Api"];
    statsUrlAggregated: SafeResourceUrl;
    statsUrlLicenses: SafeResourceUrl;
    statsUrlMaterials: SafeResourceUrl;
    tab:String = 'LICENSES';
    constructor(
        private router : Router,
        private toast: Toast,
        private route : ActivatedRoute,
        private title : Title,
        private config : ConfigurationService,
        private session : SessionStorageService,
        private translate : TranslateService,
        private http:HttpClient,
        private sanitizer: DomSanitizer,
        private configService:ConfigurationService,
        private network : RestNetworkService) {
        Translation.initialize(translate, this.config, this.session, this.route).subscribe(() => {
            UIHelper.setTitle('SERVICES.TITLE', title, translate, config);
        });


        this.configService.getAll().subscribe((data: any) => {
            this.refreshServiceList();
        });
    }


    public registerService() {
        this.getJSON().subscribe(
            (data:any) => {
                this.network.addService(data).subscribe((data:any) => {
                    this.toast.toast("SERVICES.REGISTERED");
                    this.refreshServiceList();
                    this.serviceUrl = '';
                }, (error:any) => {
                    let message = "SERVICES.ERROR.REGISTERED";
                    //we cannot filter the status
                    if(error._body.includes('Duplicate child name not allowed'))
                        message = "SERVICES.ERROR.DUPLICATE";
                    this.toast.error(null, message);
                })
            },
            (error:any) => {this.toast.error(null, 'SERVICES.ERROR.LOADJSON')});
    }


    public getJSON(): Observable<any> {
        return this.http.get(this.serviceUrl)
            .map((res:any) => res.json());
    }

    hasInterface(service:Service, type:string) {
        for(let i of service.interfaces) {
            if(i.type == type) {
                return i;
            }
        }
        return false;
    }

    setChart(service:Service) {
        if(service.statisticsInterface) {
            if(service.active) {
                service.active = false;
                this.statsUrlLicenses = this.statsUrlAggregated;
                this.statsUrlMaterials = '';
                return;
            }
            this.statsUrlLicenses = this.sanitizer.bypassSecurityTrustResourceUrl(this.configService.instant('services.visualization') + '?charts=licenses&statsUrl=' + service.statisticsInterface);
            this.statsUrlMaterials = this.sanitizer.bypassSecurityTrustResourceUrl(this.configService.instant('services.visualization') + '?charts=formats&statsUrl=' + service.statisticsInterface + '?subGroup=fileFormat');

            for(let s of this.registeredServices) {
                s.active = false;
            }
            service.active = true;
        }
    }

    private refreshServiceList() {
        this.network.getServices().subscribe((data:Service[])=>{
            this.stats.all={
                label:"Alle",
                count:0
            };
            let registeredServicesRaw = data;
            for(let service of registeredServicesRaw) {
                if(service.interfaces) {
                    for (let _interface of service.interfaces) {
                        if(_interface.type == 'Statistics') {
                            service.statisticsInterface = _interface.url;
                            this.loading++;
                            this.network.getStatistics(service.statisticsInterface + '?subGroup=fileFormat').subscribe((data: any) => {
                                this.stats.all.count += data.overall.count;
                                for (let c of data.groups) {
                                    if (!this.stats[c.key]) {
                                        this.stats[c.key]= {
                                            label: c.displayName,
                                            count: 0
                                        };
                                    }
                                    this.stats[c.key].count += c.count;
                                }
                                this.setAggregatedStats();
                            }, (error: any) => {
                                this.setAggregatedStats();
                            })

                        }
                    }
                }
            }
            this.registeredServices = registeredServicesRaw;
        });
    }

    setAggregatedStats() {
        this.loading--;
        if(this.loading == 0) {
            let stats:any = {};
            stats.groups = [];
            for(let key in this.stats) {
                if(key=='all')
                    stats.overall = {count: this.stats[key].count};
                else
                    stats.groups.push({key:this.stats[key].label, displayName: this.stats[key].label, count:this.stats[key].count});
            }
            this.statsUrlAggregated = this.sanitizer.bypassSecurityTrustResourceUrl(this.configService.instant('services.visualization') + '?charts=licenses&stats=' + JSON.stringify(stats));
            this.statsUrlLicenses = this.statsUrlAggregated;
            }
    }

}
