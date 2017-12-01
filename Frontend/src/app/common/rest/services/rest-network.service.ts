import {Injectable} from "@angular/core";
import {RestConstants} from "../rest-constants";
import {Observable} from "rxjs";
import {RestConnectorService} from "./rest-connector.service";
import {IamUsers, IamAuthorities, OrganizationOrganizations, NetworkRepositories, Repository,Node} from "../data-object";
import {Response} from "@angular/http";

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

  static isFromHomeRepo(node: Node) {
    return node.ref.repo==RestConstants.HOME_REPOSITORY || node.ref.isHomeRepo;
  }
}
