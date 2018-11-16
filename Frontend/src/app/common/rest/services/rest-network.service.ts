import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations, NetworkRepositories, Repository, Node, Application} from "../data-object";
import {Helper} from "../../helper";
import {AbstractRestService} from "./abstract-rest-service";
import {Service} from "../data-object";

@Injectable()
export class RestNetworkService extends AbstractRestService{
  public static supportsImport(repository: string, repositories: Repository[]) {
    if(repositories==null)
      return false;
      for(let r of repositories){
        if(r.id==repository) {
          return r.repositoryType=='PIXABAY';
        }
      }
      return false;
  }
  static allFromHomeRepo(nodes: Node[],repositories:Repository[]) {
    for(let node of nodes) {
      if (!RestNetworkService.isHomeRepo(node.ref.repo, repositories))
        return false;
    }
    return true;
  }
  static isFromHomeRepo(node: Node,repositories:Repository[]) {
    if(node.ref && node.ref.isHomeRepo)
      return true;
    return RestNetworkService.isHomeRepo(node.ref.repo,repositories);
  }

  static getRepositoryById(id: string, repositories: Repository[]) {
    let i=Helper.indexOfObjectArray(repositories,'id',id);
    if(id==RestConstants.HOME_REPOSITORY){
      i=Helper.indexOfObjectArray(repositories,'isHomeRepo',true);
    }
    if(i==-1)
      return null;
    return repositories[i];
  }

  static isHomeRepo(repositoryId: string, repositories: Repository[]) {
    if(repositoryId==RestConstants.HOME_REPOSITORY)
      return true;
    if(!repositories)
      return false;
    let repository=RestNetworkService.getRepositoryById(repositoryId,repositories);
    if(repository){
      return repository.isHomeRepo;
    }
    return false;
  }
    constructor(connector : RestConnectorService) {
        super(connector);
    }
    public getRepositories = () => {
        let query = this.connector.createUrl("network/:version/repositories",null);
        return this.connector.get<NetworkRepositories>(query, this.connector.getRequestOptions());
    }


    public addService = (jsondata:string): Observable<any> => {
        let query=this.connector.createUrl("network/:version/services",null);
        return this.connector.post<any>(query,jsondata,this.connector.getRequestOptions());
    }

    public getServices = () => {
        let query=this.connector.createUrl("network/:version/services",null);
        return this.connector.get<Service[]>(query,this.connector.getRequestOptions())
    }

    public getStatistics = (url:string) => {
        return this.connector.get<any>(url,this.connector.getRequestOptions(), false);
    }

}
