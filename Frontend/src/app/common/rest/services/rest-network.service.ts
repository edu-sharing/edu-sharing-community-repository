import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations, NetworkRepositories, Repository,Node} from "../data-object";
import {Response} from "@angular/http";
import {Helper} from "../../helper";

@Injectable()
export class RestNetworkService {
  constructor(private connector: RestConnectorService) {
  }
  public getRepositories = (): Observable<NetworkRepositories> => {
    let query = this.connector.createUrl("network/:version/repositories",null);
    return this.connector.get(query, this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }

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
}
