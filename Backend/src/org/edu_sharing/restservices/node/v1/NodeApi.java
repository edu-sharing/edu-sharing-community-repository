package org.edu_sharing.restservices.node.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.*;
import org.edu_sharing.restservices.shared.ACL;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.NodeRemote;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.User;

import org.edu_sharing.service.clientutils.ClientUtilsService;
import org.edu_sharing.service.clientutils.WebsiteInformation;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.editlock.LockedException;
import org.edu_sharing.service.nodeservice.AssocInfo;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.share.ShareService;
import org.glassfish.jersey.media.multipart.FormDataParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/node/v1")
@Api(tags = {"NODE v1"})
@ApiService(value="NODE", major=1, minor=0)
public class NodeApi  {

	
	private static Logger logger = Logger.getLogger(NodeApi.class);
	  @GET
	    @Path("/nodes/{repository}/{node}/workflow")
	        
	    @ApiOperation(
	    	value = "Get workflow history.", 
	    	notes = "Get workflow history of node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = WorkflowHistory[].class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response getWorkflowHistory(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		
	    		
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	
		    	return Response.status(Response.Status.OK).entity(nodeDao.getWorkflowHistory()).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }
	  @POST
	    @Path("/nodes/{repository}/{node}/report")
	        
	    @ApiOperation(
	    	value = "Report the node.", 
	    	notes = "Report a node to notify the admin about an issue)")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response reportNode(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "the reason for the report",required=true ) @QueryParam("reason") String reason,
	    	@ApiParam(value = "mail of reporting user",required=true ) @QueryParam("userEmail") String userEmail,
	    	@ApiParam(value = "additional user comment",required=false ) @QueryParam("userComment") String userComment,
			@Context HttpServletRequest req) {
	    	
	    	try {
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.reportNode(reason,userEmail,userComment);
		    	
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }
	  @PUT
	    @Path("/nodes/{repository}/{node}/workflow")
	        
	    @ApiOperation(
	    	value = "Add workflow.", 
	    	notes = "Add workflow entry to node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response addWorkflowHistory(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "The history entry to put (editor and time can be null and will be filled automatically)", required = true) WorkflowHistory entry,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    			    		
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.addWorkflowHistory(entry);
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }

	@POST
	@Path("/nodes/{repository}/{node}/publish")

	@ApiOperation(
			value = "Publish",
			notes = "Create a published copy of the current node ")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response publishCopy(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = "handle mode, if a handle should be created. Skip this parameter if you don't want an handle",required=false ) @QueryParam("handleMode") HandleMode handleMode,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao published = nodeDao.publishCopy(handleMode);
			NodeEntry response = new NodeEntry();
			response.setNode(published.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@GET
	@Path("/nodes/{repository}/{node}/publish")

	@ApiOperation(
			value = "Publish",
			notes = "Get all published copies of the current node")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntries.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getPublishedCopies(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao originalDao = NodeDao.getNode(repoDao, node);
			List<NodeDao> published = originalDao.getPublishedCopies();
			NodeEntries response = new NodeEntries();
			response.setNodes(published.stream().map(dao -> {
				try {
					return dao.asNode();
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList()));

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@PUT
	    @Path("/nodes/{repository}/{node}/aspects")
	        
	    @ApiOperation(
	    	value = "Add aspect to node.", 
	    	notes = "Add aspect to node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response addAspects(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "aspect name, e.g. ccm:lomreplication",required=true) List<String> aspects,
			@Context HttpServletRequest req) {
	    	
	    	try {
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.addAspects(aspects);
		    	NodeEntry response = new NodeEntry();
		    	response.setNode(nodeDao.asNode());
		    	
		    	return Response.status(Response.Status.OK).entity(response).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);    		
	    	}

	    }
	  
    @GET
    @Path("/nodes/{repository}/{node}/metadata")
        
    @ApiOperation(
    	value = "Get metadata of node.", 
    	notes = "Get metadata of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getMetadata(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    	
    	try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);
			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getMetadata(remote.getRepository(), remote.getNodeId(), propertyFilter, req);
			}
    		Filter filter = new Filter(propertyFilter);
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(nodeDao.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }


    @GET
    @Path("/nodes/{repository}/{node}/lock/unlock")
        
    @ApiOperation(
    	value = "unlock node.", 
    	notes = "unlock node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response unlock(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Context HttpServletRequest req) {
    	
    	try{
    		EditLockServiceFactory.getEditLockService().unlock(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node));
    	}catch(LockedException e){
    		return ErrorResponse.createResponse(e);    		
    	}
    	return Response.ok().build();
    }
    
    @GET
    @Path("/nodes/{repository}/{node}/lock/status")
        
    @ApiOperation(
    	value = "locked status of a node.", 
    	notes = "locked status of a node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeLocked.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response islocked(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@Context HttpServletRequest req) {
		try {
			boolean isLocked = EditLockServiceFactory.getEditLockService().isLockedByAnotherUser(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node));
			NodeLocked response = new NodeLocked();
			response.setLocked(isLocked);

			return Response.ok().entity(response).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    
    
    @GET
    @Path("/nodes/{repository}/{node}/textContent")
        
    @ApiOperation(
    	value = "Get the text content of a document.", 
    	notes = "May fails with 500 if the node can not be read.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeText.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getTextContent(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    	
    	try {
    		    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	
	    	NodeText response = new NodeText();
	    	response.setText(((MCAlfrescoAPIClient)repoDao.getBaseClient()).getNodeTextContent(node,MimetypeMap.MIMETYPE_TEXT_PLAIN));
	    	try{
	    		response.setHtml(((MCAlfrescoAPIClient)repoDao.getBaseClient()).getNodeTextContent(node,MimetypeMap.MIMETYPE_HTML));
	    	}catch(Throwable t){}
	    	try{
	    		InputStream is=((MCAlfrescoAPIClient)repoDao.getBaseClient()).getContent(node);
	    		if(is.available()<1024*1024*5)
	    			response.setRaw(IOUtils.toString(is));
	    	}catch(Throwable t){}
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }
    @GET
    @Path("/nodes/{repository}/{node}/parents")
        
    @ApiOperation(
    	value = "Get parents of node.", 
    	notes = "Get all parents metadata + own metadata of node. Index 0 is always the current node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ParentEntries.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getParents(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
    	@ApiParam(value = "activate to return the full alfresco path, otherwise the path for the user home is resolved") @QueryParam("fullPath") Boolean fullPath,
		@Context HttpServletRequest req) {
    	
    	try {
    		
    		Filter filter = new Filter(propertyFilter);
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	if("-inbox-".equals(node)){
    			node = repoDao.getUserInbox();
    		}
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	ParentEntries response=new ParentEntries();
	    	List<Node> parents=new ArrayList<>();
	    	Node last=nodeDao.asNode();
	    	parents.add(last);
	    	String userHome=repoDao.getUserHome();
	    	List<NodeRef> shared = PersonDao.getPerson(repoDao,PersonDao.ME).asPerson().getSharedFolders();
	    	boolean collection=last.getMediatype().equals("collection");
	    	if(fullPath==null)
	    		fullPath=false;
	    	while(true){
	    		if(last==null || last.getParent()==null || last.getParent().getId()==null)
	    			break;
	    		if(!fullPath){
	    			if(last.getParent().getId().equals(userHome)){
	    				response.setScope("MY_FILES");
	    				break;
	    			}
	    			if((shared!=null && shared.contains(last.getRef()))){
	    				response.setScope("SHARED_FILES");
	    				break;
	    			}
	    		}
	    		if(collection && !fullPath){
					Node finalLast = last;
					last=AuthenticationUtil.runAsSystem(()-> NodeDao.getNode(repoDao, finalLast.getParent().getId(),filter).asNode());
					if(!last.getMediatype().equals("collection")){
	    				response.setScope("COLLECTION");
	    				break;
	    			}
	    		}
	    		else{
					last=NodeDao.getNode(repoDao, last.getParent().getId(),filter).asNode();
				}
	    		parents.add(last);
	    	}
	    	
	    	response.setNodes(parents);
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);    		
    	}

    }
    
    @PUT
    @Path("/nodes/{repository}/{node}/metadata")    
    
    @ApiOperation(
    	value = "Change metadata of node.", 
    	notes = "Change metadata of node.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		})

    public Response changeMetadata(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "properties" ,required=true ) HashMap<String, String[]> properties,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeProperties(properties);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }

	@GET
	@Path("/nodes/{repository}/{node}/metadata/template")

	@ApiOperation(
			value = "Get the metadata template + status for this folder.",
			notes = "All the given metadata will be inherited to child nodes.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getTemplateMetadata(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);


			NodeTemplateEntry response = new NodeTemplateEntry();
			if(nodeDao.getTemplateNode()!=null)
				response.setNode(nodeDao.getTemplateNode().asNode());
			response.setEnabled(nodeDao.getTemplateStatus());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {

			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/nodes/{repository}/{node}/metadata/template")

	@ApiOperation(
			value = "Set the metadata template for this folder.",
			notes = "All the given metadata will be inherited to child nodes.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response changeTemplateMetadata(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = "Is the inherition currently enabled",required=true ) @QueryParam("enable") Boolean enable,
			@ApiParam(value = "properties" ,required=true ) HashMap<String, String[]> properties,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);

			NodeDao templateNode = nodeDao.changeTemplateProperties(enable,properties);

			NodeTemplateEntry response = new NodeTemplateEntry();
			if(nodeDao.getTemplateNode()!=null)
				response.setNode(templateNode.asNode());
			response.setEnabled(nodeDao.getTemplateStatus());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {

			return ErrorResponse.createResponse(t);
		}
	}


	@POST
    @Path("/nodes/{repository}/{node}/metadata")    
    
    @ApiOperation(
    	value = "Change metadata of node (new version).", 
    	notes = "Change metadata of node (new version).")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),	        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	})

    public Response changeMetadataWithVersioning(
	    @ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "comment",required=true) @QueryParam("versionComment") String versionComment,
	    @ApiParam(value = "properties" ,required=true ) HashMap<String, String[]> properties,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changePropertiesWithVersioning(properties, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/metadata")
    @ApiOperation(hidden = true, value = "")

    public Response options01() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, PUT").build();
    }
    
    @DELETE
    @Path("/nodes/{repository}/{node}")    
    
    @ApiOperation(
    	value = "Delete node.", 
    	notes = "Delete node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response delete(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-") @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "move the node to recycle",defaultValue="true",required=false) @QueryParam("recycle") Boolean recycle,
    	@ApiParam(value = "protocol",defaultValue="",required=false) @QueryParam("protocol") String protocol,
    	@ApiParam(value = "store",defaultValue="",required=false) @QueryParam("store") String store,
		@Context HttpServletRequest req) {
    	
    	try {
    		
    		if(protocol != null && !protocol.trim().equals("") 
    				&& store != null && !store.trim().equals("")) {
    			
    			if(!ApplicationInfoList.getHomeRepository().getUsername().equals(AuthenticationUtil.getFullyAuthenticatedUser())){
    				throw new Exception("admin user required when trying to delete node of another store");
    			}
    			
    			NodeDao.delete(protocol, store, node);
    			return Response.status(Response.Status.OK).build();
    		
    		}
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.delete(recycle==null ? true : recycle);
	    	
	    	return Response.status(Response.Status.OK).build();

    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}")
    @ApiOperation(hidden = true, value = "")

    public Response options02() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, DELETE").build();
    }

    @GET
    @Path("/nodes/{repository}/{node}/children")    
    
    @ApiOperation(
    	value = "Get children of node.", 
    	notes = "Get children of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntries.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getChildren(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = "ID of parent node (or \"-userhome-\" for home directory of current user, \"-shared_files-\" for shared folders, \"-to_me_shared_files\" for shared files for the user,\"-my_shared_files-\" for files shared by the user, \"-inbox-\" for the inbox, \"-workflow_receive-\" for files assigned by workflow, \"-saved_search-\" for saved searches of the user)",required=true ) @PathParam("node") String node,
	    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
	    @ApiParam(value = RestConstants.MESSAGE_FILTER) @QueryParam("filter") List<String> filter,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    @ApiParam(value = "Filter for a specific association. May be empty",required = false,defaultValue = "") @QueryParam("assocName") String assocName,
	    @ApiParam(value = RestConstants.MESSAGE_PROPERTY_FILTER, defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {

    	try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);
			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getChildren(remote.getRepository(), remote.getNodeId(), maxItems, skipCount, filter, sortProperties, sortAscending, assocName, propertyFilter, req);
			}

    		Filter propFilter = new Filter(propertyFilter);
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	node=NodeDao.mapNodeConstants(repoDao,node);

			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);

			NodeEntries response=null;
			List<NodeRef> children=null;
			if("-shared_files-".equals(node)){
		    	User person = PersonDao.getPerson(repoDao, PersonDao.ME).asPerson();
		    	children = person.getSharedFolders();
		    	List<org.alfresco.service.cmr.repository.NodeRef> converted=NodeDao.convertApiNodeRef(children);
                children=NodeDao.convertAlfrescoNodeRef(repoDao,NodeDao.sortAlfrescoRefs(converted,filter,sortDefinition));
            }
	    	else if("-my_shared_files-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedByMe(repoDao, filter, propFilter, sortDefinition, skipCount, maxItems));
			}
	    	else if("-workflow_receive-".equals(node)){
	    		children = NodeDao.getWorkflowReceive(repoDao,filter,sortDefinition);
	    	}
	    	else if("-to_me_shared_files-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedToMe(repoDao, SharedToMeType.All, filter, propFilter,sortDefinition,skipCount,maxItems));
			}
			else if("-to_me_shared_files_personal-".equals(node)){
				response = searchResultToResponse(NodeDao.getFilesSharedToMe(repoDao, SharedToMeType.Private, filter, propFilter,sortDefinition,skipCount,maxItems));
			}
	    	else if("-frontpage-".equals(node)){
				children = NodeDao.getFrontpageNodes(repoDao);
			}
	    	else{
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, propFilter);
	    		children = nodeDao.getChildren(assocName,filter,sortDefinition);
            }
	    	if(response==null)
            	response=NodeDao.convertToRest(repoDao,propFilter,children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);
			//List<Node> sorted=NodeDao.sortAndFilterByType(repoDao,children,sortDefinition,filter,propFilter);
	    	//Collections.sort(children);
			//NodeEntries response=createResponseFromNodeList(sorted,skipCount,maxItems);
	    
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }

	public NodeEntries searchResultToResponse(org.edu_sharing.restservices.shared.SearchResult<NodeDao> data) throws DAOException {
		NodeEntries response;
		response=new NodeEntries();
		response.setPagination(data.getPagination());
		List<Node> list = new ArrayList<>();
		for (NodeDao nodeDao : data.getNodes()) {
			Node asNode = nodeDao.asNode();
			list.add(asNode);
		}
		response.setNodes(list);
		return response;
	}

	@GET
	@Path("/nodes/{repository}/{node}/assocs")

	@ApiOperation(
			value = "Get related nodes.",
			notes = "Get nodes related based on an assoc.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntries.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getAssocs(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "Either where the given node should be the \"SOURCE\" or the \"TARGET\"",required = true) @QueryParam("direction") AssocInfo.Direction direction,
			@ApiParam(value = "Association name (e.g. ccm:forkio).") @QueryParam("assocName") String assocName,
			@ApiParam(value = RestConstants.MESSAGE_PROPERTY_FILTER, defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			Filter propFilter = new Filter(propertyFilter);

			NodeEntries response=new NodeEntries();
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			List<NodeRef> children;

            SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);

            NodeDao nodeDao = NodeDao.getNode(repoDao, node, propFilter);
			children = nodeDao.getAssocs(new AssocInfo(direction,assocName),null,sortDefinition);
            response=NodeDao.convertToRest(repoDao,propFilter,children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);

			return Response.status(Response.Status.OK).entity(response).build();

		}
		catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
    @DELETE
    @Path("/nodes/{repository}/{node}/shares/{shareId}")    
    
    @ApiOperation(
    	value = "Remove share of a node.", 
    	notes = "Remove the specified share id")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response removeShare(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "share id",required=true ) @PathParam("shareId") String shareId,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.removeShare(shareId);
	    	return Response.status(Response.Status.OK).build();
    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @POST
    @Path("/nodes/{repository}/{node}/shares/{shareId}")    
    
    @ApiOperation(
    	value = "update share of a node.", 
    	notes = "update the specified share id")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeShare.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response updateShare(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "share id",required=true ) @PathParam("shareId") String shareId,
	    @ApiParam(value = "expiry date for this share, leave empty or -1 for unlimited",required=false,defaultValue=""+ShareService.EXPIRY_DATE_UNLIMITED ) @QueryParam("expiryDate") Long expiryDate,
	    @ApiParam(value = "new password for share, leave empty if you don't want to change it",required=false,defaultValue="") @QueryParam("password") String password,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	
	    	NodeShare response=nodeDao.updateShare(shareId,expiryDate==null?ShareService.EXPIRY_DATE_UNLIMITED:expiryDate,password);
	    	return Response.status(Response.Status.OK).entity(response).build();
    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }  
    @POST
    @Path("/nodes/{repository}/{node}/import")    
    
    @ApiOperation(
    	value = "Import node", 
    	notes = "Import a node from a foreign repository to the local repository.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response importNode(
    	@ApiParam(value = "The id of the foreign repository",required=true) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "Parent node where to store it locally, may also use -userhome- or -inbox-",required=true ) @QueryParam("parent") String parent,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	if(repoDao.isHomeRepo()){
	    		throw new InvalidArgumentException("Import can be only done for a foreign repository");
	    	}
	    	
	    	RepositoryDao repoDaoHome = RepositoryDao.getRepository(RepositoryDao.HOME);
			node=NodeDao.mapNodeConstants(repoDaoHome,node);
			parent=NodeDao.mapNodeConstants(repoDaoHome,parent);

			NodeDao nodeDao=NodeDao.getNode(repoDao, node).importNode(parent);
	    	NodeEntry response=new NodeEntry();
	    	response.setNode(nodeDao.asNode());
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @GET
    @Path("/nodes/{repository}/{node}/shares")    
    
    @ApiOperation(
    	value = "Get shares of node.", 
    	notes = "Get list of shares (via mail/token) for a node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeShare[].class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getShares(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "Filter for a specific email or use "+ShareService.EMAIL_TYPE_LINK+" for link shares (Optional)",required=false) @QueryParam("email") String email,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	List<NodeShare> response=nodeDao.getShares(email);
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    @PUT
    @Path("/nodes/{repository}/{node}/shares")    
    
    @ApiOperation(
    	value = "Create a share for a node.", 
    	notes = "Create a new share for a node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeShare.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response createShare(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "expiry date for this share, leave empty or -1 for unlimited",required=false,defaultValue=""+ShareService.EXPIRY_DATE_UNLIMITED ) @QueryParam("expiryDate") Long expiryDate,
	    @ApiParam(value = "password for this share, use none to not use a password",required=false,defaultValue="") @QueryParam("password") String password,
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao=NodeDao.getNode(repoDao, node);
	    	NodeShare response=nodeDao.createShare(expiryDate==null ? ShareService.EXPIRY_DATE_UNLIMITED : expiryDate,password);
	    	return Response.status(Response.Status.OK).entity(response).build();

    	}
    	catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
       
    }    
    public static NodeEntries createResponseFromNodeList(List<Node> sorted, Integer skipCount, Integer maxItems) {
		NodeEntries response = new NodeEntries();
    	int min = (skipCount != null) ? Math.min(sorted.size(), skipCount) : 0;
    	int max = (maxItems != null) ? Math.min(sorted.size(), min + maxItems) : sorted.size();   
    		
    	List<Node> data = new ArrayList<Node>();
    	for (Node child : sorted.subList(min, max)) {
    		data.add(child);
    	}
    	
    	Pagination pagination = new Pagination();
    	pagination.setFrom(min);
    	pagination.setCount(data.size());
    	pagination.setTotal(sorted.size());
    	
    	response.setNodes(data);
    	response.setPagination(pagination);
    	return response;
	}


	

	@POST
    @Path("/nodes/{repository}/{node}/children")    
    
    @ApiOperation(
    	value = "Create a new child.", 
    	notes = "Create a new child.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response createChild(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_PARENT_NODE+" use -userhome- for userhome or -inbox- for inbox node",required=true ) @PathParam("node") String node,
	    @ApiParam(value = "type of node",required=true ) @QueryParam("type") String type,
	    @ApiParam(value = "aspects of node" ) @QueryParam("aspects") List<String> aspects,
	    @ApiParam(value = "rename if the same node name exists",required=false,defaultValue="false") @QueryParam("renameIfExists") Boolean renameIfExists,
	    @ApiParam(value = "comment, leave empty = no inital version", required=false ) @QueryParam("versionComment")  String versionComment,
	    @ApiParam(value = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}" , required=true ) HashMap<String, String[]> properties,	    
	    @ApiParam(value = "Association type, can be empty" , required=false ) @QueryParam("assocType") String assocType,	    
		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	WebsiteInformation websiteInformation = resolveURLTitle(properties);
	    	NodeDao child = nodeDao.createChild(type, aspects, properties,
	    			renameIfExists==null ? false : renameIfExists.booleanValue(),
					assocType!=null && !assocType.trim().isEmpty() ? assocType : null);

	    	if(websiteInformation != null && websiteInformation.getTwitterImage() != null
					&& !websiteInformation.getTwitterImage().trim().isEmpty()){
				InputStream inputStream = null;
	    		try {
					child.changePreview(inputStream = new URL(websiteInformation.getTwitterImage()).openStream(), "");
				}catch(IOException e){}
	    		finally {
	    			if(inputStream != null) inputStream.close();
				}
			}
	    	
			if(versionComment!=null && !versionComment.isEmpty()){
				child.createVersion(versionComment);
			}
	    	
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }


	@POST
	@Path("/nodes/{repository}/{node}/xapi")

	@ApiOperation(
			value = "Store xApi-Conform data for a given node")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Object.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response storeXApiData(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = "xApi conform json data",required=true ) String xApi,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			String result = nodeDao.storeXApiData(xApi);
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	public WebsiteInformation resolveURLTitle(HashMap<String, String[]> properties) {
		String[] url=(String[])properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_WWWURL));
		if(url==null)
			return null;
		// Don't resolve url if name is already given by client
		if(properties.get(CCConstants.getValidLocalName(CCConstants.CM_NAME))!=null) {
			properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME),
					new String[]{NodeServiceHelper.cleanupCmName(properties.get(CCConstants.getValidLocalName(CCConstants.CM_NAME))[0])});
			return null;
		}
		 WebsiteInformation info=ClientUtilsService.getWebsiteInformation(url[0]);
		 if(info==null){
		     properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{NodeServiceHelper.cleanupCmName(url[0])});
		     properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE),url);
			 return null;
		 }
		 String title=info.getTitle();
		 if(info.getTitle()==null) {
			 title = info.getPage();
		 }
		 if(title == null || title.trim().isEmpty()){
		 	title = url[0];
		 }
	    properties.put(CCConstants.getValidLocalName(CCConstants.CM_NAME), new String[]{NodeServiceHelper.cleanupCmName(title)});
	    properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_TITLE),new String[]{title});
	    if(info.getDescription()!=null)
	    	properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_DESCRIPTION),new String[]{info.getDescription()});
	    if(info.getKeywords()!=null)
	    	properties.put(CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_KEYWORD),info.getKeywords());
	    if(info.getLicense()!=null){
	    	properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),new String[]{info.getLicense().getName()});
	    	if(info.getLicense().getCcVersion()!=null)
	    		properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION),new String[]{info.getLicense().getCcVersion()});
		}
	    if(info.getLrmiProperties()!=null){
	    	properties.putAll(info.getLrmiProperties());
		}
	    return info;
	}
	@OPTIONS    
    @Path("/nodes/{repository}/{node}/children")
    @ApiOperation(hidden = true, value = "")

    public Response options03() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST").build();
    }
	@POST
	@Path("/nodes/{repository}/{node}/children/_fork")

	@ApiOperation(
			value = "Create a copy of a node by creating a forked version (variant)."
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response createForkOfNode(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
			@ApiParam(value = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
			@ApiParam(value = "flag for children",required=true) @QueryParam("withChildren") boolean withChildren,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createFork(source);

			NodeEntry response = new NodeEntry();
			response.setNode(child.asNode());

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
    @Path("/nodes/{repository}/{node}/children/_copy")    
    
    @ApiOperation(
    	value = "Create a new child by copying.", 
    	notes = "Create a new child by copying.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response createChildByCopying(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
	    @ApiParam(value = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
	    @ApiParam(value = "flag for children",required=true) @QueryParam("withChildren") boolean withChildren,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createChildByCopy(source, withChildren);
	    			
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {	
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/children/_copy")
    @ApiOperation(hidden = true, value = "")

    public Response options04() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }
    
    @POST
    @Path("/nodes/{repository}/{node}/children/_move")    
    
    @ApiOperation(
    	value = "Create a new child by moving.", 
    	notes = "Create a new child by moving.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response createChildByMoving(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
		@ApiParam(value = RestConstants.MESSAGE_PARENT_NODE,required=true ) @PathParam("node") String node,
		@ApiParam(value = RestConstants.MESSAGE_SOURCE_NODE,required=true) @QueryParam("source") String source,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			source=NodeDao.mapNodeConstants(repoDao,source);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
			NodeDao child = nodeDao.createChildByMove(source);
	    			
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(child.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
        
    @OPTIONS    
    @Path("/nodes/{repository}/{node}/children/_move")
    @ApiOperation(hidden = true, value = "")

    public Response options05() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }
    @POST
    @Path("/nodes/{repository}/{node}/preview")
    @Consumes({ "multipart/form-data" })
    
    @ApiOperation(
    	value = "Change preview of node.", 
    	notes = "Change preview of node.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    })

    public Response changePreview(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @FormDataParam("image") InputStream inputStream,
	    @ApiParam(value = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changePreview(inputStream,mimetype);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
	@DELETE
	@Path("/nodes/{repository}/{node}/preview")

	@ApiOperation(
			value = "Delete preview of node.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response deletePreview(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);

			NodeDao newNode = nodeDao.deletePreview();

			NodeEntry response = new NodeEntry();
			response.setNode(newNode.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
    @POST
    @Path("/nodes/{repository}/{node}/content")
    @Consumes({ "multipart/form-data" })
    
    @ApiOperation(
    	value = "Change content of node.", 
    	notes = "Change content of node.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    })

    public Response changeContent(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "comment, leave empty = no new version, otherwise new version is generated", required=false ) @QueryParam("versionComment")  String versionComment,
	    @ApiParam(value = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
	    @FormDataParam("file") InputStream inputStream,
//	    @FormDataParam("file") FormDataContentDisposition fileDetail,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeContent(inputStream, mimetype, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	}catch(DAOVirusDetectedException t){
			logger.warn(t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
    
    @POST
    @Path("/nodes/{repository}/{node}/textContent")
    @Consumes({ "multipart/form-data" })
    
    @ApiOperation(
    	value = "Change content of node as text.", 
    	notes = "Change content of node as text.")
    
    @ApiResponses(
    	value = { 
        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    })

    public Response changeContentAsText(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @ApiParam(value = "comment, leave empty = no new version, otherwise new version is generated", required=false ) @QueryParam("versionComment")  String versionComment,
	    @ApiParam(value = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
	    @ApiParam(value = "The content data to write (text)", required=true )String text,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao newNode = nodeDao.changeContent(new ByteArrayInputStream(text.getBytes()), mimetype, versionComment);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(newNode.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/content")
    @ApiOperation(hidden = true, value = "")

	public Response options06() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}

    @GET
    @Path("/nodes/{repository}/{node}/versions")    
    
    @ApiOperation(
    	value = "Get all versions of node.", 
    	notes = "Get all versions of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeVersionRefEntries.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getVersions(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, Filter.createShowAllFilter());
	    	
	    	NodeVersionEntries response=new NodeVersionEntries();
	    	response.setVersions(nodeDao.getHistory());
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

   }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions")
    @ApiOperation(hidden = true, value = "")

	public Response options07() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

    @GET
    @Path("/nodes/{repository}/{node}/versions/{major}/{minor}/metadata")    
    
    @ApiOperation(
    	value = "Get metadata of node version.", 
    	notes = "Get metadata of node version.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeVersionEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getVersionMetadata(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "major version",required=true ) @PathParam("major") int major,
    	@ApiParam(value = "minor version",required=true ) @PathParam("minor") int minor,
    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Filter filter = new Filter(propertyFilter);
	    	
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node, filter);
	    	
	    	NodeVersionEntry response = new NodeVersionEntry(); 
	    	response.setVersion(nodeDao.getVersion(major, minor));
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions/{major}/{minor}/metadata")
    @ApiOperation(hidden = true, value = "")

	public Response options08() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

    @PUT
    @Path("/nodes/{repository}/{node}/versions/{major}/{minor}/_revert")    
    
    @ApiOperation(
    	value = "Revert to node version.", 
    	notes = "Revert to node version.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response revertVersion(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "major version",required=true ) @PathParam("major") int major,
    	@ApiParam(value = "minor version",required=true ) @PathParam("minor") int minor,
		@Context HttpServletRequest req) {
    
    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodeDao revert = nodeDao.revertHistory(major, minor);
	    	
	    	NodeEntry response = new NodeEntry();
	    	response.setNode(revert.asNode());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/versions/{major}/{minor}/_revert")    
	@ApiOperation(hidden = true, value = "")

	public Response options09() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
	}

    @POST
    @Path("/nodes/{repository}")    
    
    @ApiOperation(
    	value = "Searching nodes.", 
    	notes = "Searching nodes.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response getNodes(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    @ApiParam(value = "lucene query",required=true) @QueryParam("query") String query,
	    @ApiParam(value = "facettes") @QueryParam("facettes") List<String> facettes,
	    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="10") @QueryParam("maxItems") Integer maxItems,
	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    @ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
		    
    	try {
    		
    		Filter filter= new Filter(propertyFilter);
  
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    		
    		SearchToken searchToken=new SearchToken();
			searchToken.setLuceneString(query);
			searchToken.setFrom(skipCount != null ? skipCount : 0);
			searchToken.setMaxResult(maxItems!= null ? maxItems : 10);
			searchToken.setFacettes(facettes);
			searchToken.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
    		NodeSearch search = NodeDao.search(repoDao,searchToken);
    		List<Node> data = new ArrayList<Node>();
	    	for (NodeRef ref : search.getResult()) {
	    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
	    	}
	    	Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());
	    	
	    	
	    	SearchResult response = new SearchResult();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacettes(search.getFacettes());
	    	return Response.status(Response.Status.OK).entity(response).build();
    		
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }

    
	@OPTIONS    
	@Path("/nodes/{repository}")
    @ApiOperation(hidden = true, value = "")

	public Response options10() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
	@GET
    @Path("/nodes/{repository}/{node}/permissions/{user}")    
    
    @ApiOperation(
    	value = "Which permissions has user/group for node.", 
    	notes = "Check for actual permissions (also when user is in groups) for a specific node")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
 
    public Response hasPermission(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "Authority (user/group) to check (use \"-me-\" for current user",required=true ) @PathParam("user") String authority,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	if(authority.equals("-me-"))
	    		authority=AuthenticationUtil.getFullyAuthenticatedUser();
	    	List<String> response = nodeDao.getPermissions(authority);
	    
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	
    @GET
    @Path("/nodes/{repository}/{node}/permissions")    
    
    @ApiOperation(
    	value = "Get all permission of node.", 
    	notes = "Get all permission of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodePermissionEntry.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
 
    public Response getPermission(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			node=NodeDao.mapNodeConstants(repoDao,node);
			NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	NodePermissionEntry response = new NodePermissionEntry(); 
	    	
	    	response.setPermissions(nodeDao.getPermissions());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    @GET
    @Path("/nodes/{repository}/{node}/notifys")    
    
    @ApiOperation(
    	value = "Get notifys (sharing history) of the node.", 
    	notes = "Ordered by the time of each notify")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NotifyEntry[].class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
 
    public Response getNotifyList(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
		@Context HttpServletRequest req) {
    
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	List<NotifyEntry> notifys = nodeDao.getNotifys();
	    	return Response.status(Response.Status.OK).entity(notifys).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    @POST
    @Path("/nodes/{repository}/{node}/permissions")    
    
    @ApiOperation(
    	value = "Set local permissions of node.", 
    	notes = "Set local permissions of node.")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response setPermission(
    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
    	@ApiParam(value = "permissions",required=true ) ACL permissions,
    	@ApiParam(value = "mailtext",required=false ) @QueryParam("mailtext")  String mailText,
    	@ApiParam(value = "sendMail",required=true ) @QueryParam("sendMail") Boolean sendMail,
    	@ApiParam(value = "sendCopy",required=true ) @QueryParam("sendCopy") Boolean sendCopy,
		@Context HttpServletRequest req) {
    
    	try {
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
	    	
	    	nodeDao.setPermissions(permissions,mailText,sendMail,sendCopy);
	    	
	    	return Response.status(Response.Status.OK).build();
	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}

    }
    
	@OPTIONS    
	@Path("/nodes/{repository}/{node}/permissions")
    @ApiOperation(hidden = true, value = "")

	public Response options11() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, @POST").build();
	}
		
	
	  @POST
	    @Path("/nodes/{repository}/{node}/prepareUsage")    
	    
	    @ApiOperation(
	    	value = "create remote object and get properties.", 
	    	notes = "create remote object and get properties.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeRemote.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response prepareUsage(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    @Context HttpServletRequest req) {
	    
	    	try {
	    		
	    		if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
	    			return RepoProxyFactory.getRepoProxy().prepareUsage(repository, node, req);
	    		}
	    		
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeRemote nodeRemote = NodeDao.prepareUsage(repoDao.getId(), node);
		    	return Response.status(Response.Status.OK).entity(nodeRemote).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }
	    
		@OPTIONS    
		@Path("/nodes/{repository}/{node}/prepareUsage")
	    @ApiOperation(hidden = true, value = "")

		public Response options12() {
			
			return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, @POST").build();
		}
		
		
		@POST
	    @Path("/nodes/{repository}/{node}/owner")    
	    
	    @ApiOperation(
	    	value = "Set owner of node.", 
	    	notes = "Set owner of node.")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response setOwner(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "username",required=false ) @QueryParam("username")  String username,
			@Context HttpServletRequest req) {
	    
	    	try {
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.setOwner(username);   	
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }
		
		@POST
	    @Path("/nodes/{repository}/{node}/property")    
	    
	    @ApiOperation(
	    	value = "Set single property of node.", 
	    	notes = "When the property is unset (null), it will be removed")
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response setProperty(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "property",required=true ) @QueryParam("property")  String property,
	    	@ApiParam(value = "value",required=false ) @QueryParam("value")  List<String> value,
			@Context HttpServletRequest req) {
	    
	    	try {
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	NodeDao nodeDao = NodeDao.getNode(repoDao, node);
		    	nodeDao.setProperty(property, (Serializable) value);
		    	return Response.status(Response.Status.OK).build();
		
	    	} catch (DAOValidationException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOSecurityException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (DAOMissingException t) {
	    		
	    		logger.warn(t.getMessage(), t);
	    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
	    		
	    	} catch (Throwable t) {
	    		
	    		logger.error(t.getMessage(), t);
	    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	    	}

	    }
}

