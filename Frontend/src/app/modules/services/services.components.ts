
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { HttpModule } from '@angular/http';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestNetworkService} from "../../common/rest/services/rest-network.service";
import {Toast} from "../../common/ui/toast";
import {Observable} from "rxjs/Rx";
import {Http} from "@angular/http";
import {Application, Service} from "../../common/rest/data-object";


@Component({
    selector: 'app-services',
    templateUrl: 'services.component.html',
    styleUrls: ['services.component.scss'],
    providers: [HttpModule]
})



export class ServicesComponent {
    public serviceUrl:string;
    public registeredServices:Service[] = [];
    constructor(
        private router : Router,
        private toast: Toast,
        private route : ActivatedRoute,
        private title : Title,
        private config : ConfigurationService,
        private session : SessionStorageService,
        private translate : TranslateService,
        private http:Http,
        private network : RestNetworkService) {
        Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=> {
            UIHelper.setTitle('SERVICES.TITLE', title, translate, config);
        });
        this.refreshServiceList();
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

    private refreshServiceList() {
        this.network.getServices().subscribe((data:Service[])=>{
            this.registeredServices=data;
        });
    }
}
