import {Injectable} from '@angular/core';
import {Observable, Observer} from 'rxjs';
import 'rxjs/add/operator/do';
import {RestConnectorService} from "./rest-connector.service";
import {RestConstants} from "../rest-constants";
import {Connector, ConnectorList, Filetype, Node} from "../data-object";
import {RestNodeService} from "./rest-node.service";
import {AbstractRestService} from "./abstract-rest-service";
import {UIService} from "./ui.service";

@Injectable()
export class RestConnectorsService extends AbstractRestService{
    private static MODE_NONE=0;
    private static MODE_CREATE=1;
    private static MODE_EDIT=2;

    private currentList: ConnectorList;
    constructor(connector : RestConnectorService,
                public nodeApi : RestNodeService,
                public ui : UIService) {
        super(connector);
    }

    public list = (repository=RestConstants.HOME_REPOSITORY
    ) => {
        let query=this.connector.createUrl("connector/:version/connectors/:repository/list",repository);
        return this.connector.get<ConnectorList>(query,this.connector.getRequestOptions())
            .do((data)=>this.currentList=data);
    }
    public connectorSupportsEdit(node: Node) {
        let connectors=this.getConnectors();
        if(connectors==null)
            return null;
        for(let connector of connectors){
            // do not allow opening on a desktop-only connector on mobile
            if(connector.onlyDesktop && this.ui.isMobile())
                continue;
            if(!connector.hasViewMode && node.access.indexOf(RestConstants.ACCESS_WRITE)==-1)
                continue;
            if(RestConnectorsService.getFiletype(node,connector))
                return connector;
        }
        return null;
    }


    public static getFiletype(node:Node,connector:Connector,mode=this.MODE_NONE){
        for(let filetype of connector.filetypes){
            if(filetype.mimetype==node.mimetype && (mode==this.MODE_NONE || mode==this.MODE_EDIT && filetype.editable || mode==this.MODE_CREATE && filetype.creatable)) {
                if(filetype.mimetype=='application/zip'){
                    if((!filetype.ccressourceversion || filetype.ccressourceversion==node.properties[RestConstants.CCM_PROP_CCRESSOURCEVERSION])
                        && filetype.ccressourcetype==node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE]
                        && (!filetype.ccresourcesubtype || filetype.ccresourcesubtype==node.properties[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE]))
                        return filetype;
                    continue;
                }
                if(filetype.editorType && filetype.editorType!=node.properties[RestConstants.CCM_PROP_EDITOR_TYPE]){
                    continue;
                }
                return filetype;
            }
        }
        return null;
    }
    public generateToolUrl(connectorType:Connector,type:Filetype,node:Node):Observable<string> {
        return new Observable<string>((observer: Observer<string>) => {
            let send: any = {};
            send["connectorId"] = connectorType.id;
            send["nodeId"] = node.ref.id;
            if(this.connector.getBridgeService().isRunningCordova()){
                send["accessToken"]=this.connector.getBridgeService().getCordova().oauth.access_token;
            }
            let req = this.connector.getAbsoluteEndpointUrl()+"../eduservlet/connector?";
            let i=0;
            for (let param in send) {
                if (i > 0) {
                    req += "&";
                }
                req += param + "=" + encodeURIComponent(send[param]);
                i++;
            }
            observer.next(req);
            observer.complete();
        });
    }

    getConnectors() {
        if(this.currentList && this.currentList.connectors) {
            // filter connectors which are only available on desktop
            return this.currentList.connectors.filter((connector) => !connector.onlyDesktop || !this.ui.isMobile());
        }
        return null;
    }
}
