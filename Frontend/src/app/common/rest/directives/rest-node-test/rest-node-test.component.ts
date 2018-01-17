import { Component } from '@angular/core';
import {RestArchiveService} from "../../services/rest-archive.service";
import {RestConstants} from "../../rest-constants";
import {RestHelper} from "../../rest-helper";
import {
  NodeRef, NodeWrapper, NodePermissions, NodeVersions,NodeVersion,NodeList,Node
} from "../../data-object";
import {RestNodeService} from "../../services/rest-node.service";

@Component({
  selector: 'app-rest-node-test',
  templateUrl: './rest-node-test.component.html',
})
export class RestNodeTestComponent {
  public searchNodes : NodeList;
  public getChildren : NodeList;
  public createNode : NodeWrapper;
  public copyNode : NodeWrapper;
  public getHomeDirectory : NodeRef;
  public getNodeMetadata : NodeWrapper;
  public getNodePermissions : NodePermissions;
  public getNodeVersions : NodeVersions;
  public getNodeMetadataForVersion : NodeVersion;
  public nodes : Node[];
  public error : string;
  constructor(private node : RestNodeService) {
    /*node.searchNodes("*",[],0).subscribe(
      data => this.searchNodes=data,
      error => this.error=RestHelper.printError(error)
    )*/
    node.searchNodes("API").subscribe((list : NodeList)=>{
      this.nodes=list.nodes;
    });
    node.getHomeDirectory().subscribe(
      data => {
        this.getHomeDirectory = data;
        /*node.moveNode(this.getHomeDirectory.id,"48c30f46-1858-4d43-9599-4ff13dded307").subscribe(
          data => this.copyNode=data,
          error => this.error=RestHelper.printError(error)
        );*/

        let newNode=RestHelper.createNameProperty("API-File"+Math.random());
        node.createNode(this.getHomeDirectory.id,RestConstants.CCM_TYPE_IO,[],newNode).subscribe(
          data => this.createNode = data,
          error => this.error=RestHelper.printError(error)
        );
        node.editNodeMetadataNewVersion("48c30f46-1858-4d43-9599-4ff13dded307","new vers",RestHelper.createNameProperty("editNodeMetadata works 3")).subscribe(
          data => null,
          error => this.error=RestHelper.printError(error)
        );
        node.getNodeMetadata(this.getHomeDirectory.id).subscribe(
          data => this.getNodeMetadata=data,
          error => this.error=RestHelper.printError(error)
        );
        node.getNodeVersions("48c30f46-1858-4d43-9599-4ff13dded307").subscribe(
          data => this.getNodeVersions=data,
          error => this.error=RestHelper.printError(error)
        );
        node.getNodeVersions("48c30f46-1858-4d43-9599-4ff13dded307").subscribe(
          data => this.getNodeVersions=data,
          error => this.error=RestHelper.printError(error)
        );
        node.getNodeMetadataForVersion("48c30f46-1858-4d43-9599-4ff13dded307",1,0).subscribe(
          data => this.getNodeMetadataForVersion=data,
          error => this.error=RestHelper.printError(error)
        );
        /*
        node.getNodePermissions(this.getHomeDirectory.id).subscribe(
          data => {
            this.getNodePermissions = data;
            let permissions = data.permissions.localPermissions;
            permissions.permissions.push({
              authority: {authorityName: "admin", authorityType: RestConstants.AUTHORITY_EVERYONE},
              permission: RestConstants.PERMISSION_CONSUMER
            });
            node.setNodePermissions(this.getHomeDirectory.id, permissions).subscribe(
              data => null,
              error => this.error = RestHelper.printError(error)
            );
          },
              error => this.error=RestHelper.printError(error)
        );
        */
        node.getChildren(this.getHomeDirectory.id,[],{propertyFilter:[RestConstants.CM_NAME,RestConstants.CM_PROP_TITLE]}).subscribe(
          data => {this.getChildren=data;console.log(data.nodes[0].properties[RestConstants.CM_NAME][0]);},
          error => this.error=RestHelper.printError(error)
        )

      },
      error => this.error=RestHelper.printError(error)
    )

  }
  testUpload(file:File) : void {
      console.log("upload here "+event);
    this.node.uploadNodeContent(this.createNode.node.ref.id,file,"Test-File").subscribe(
      data => null,
      error => this.error=RestHelper.printError(error)
    )
  }

}
