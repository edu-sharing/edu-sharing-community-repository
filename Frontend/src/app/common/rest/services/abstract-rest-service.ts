import {RestNodeService} from "./rest-node.service";
import {RestConnectorService} from "./rest-connector.service";

/**
 * base class for all classes using the rest connector service
 */
export class AbstractRestService{
    constructor(protected connector : RestConnectorService){}
    getRestConnector(){
        return this.connector;
    }
}