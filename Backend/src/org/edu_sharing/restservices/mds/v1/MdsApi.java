package org.edu_sharing.restservices.mds.v1;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.MdsDaoV2;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.mds.v1.model.MdsEntries;
import org.edu_sharing.restservices.mds.v1.model.MdsEntriesV2;
import org.edu_sharing.restservices.mds.v1.model.MdsEntry;
import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.mds.v1.model.Suggestions;
import org.edu_sharing.restservices.mds.v1.model.ValueParameters;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.MdsV2;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;

@Path("/mds/v1")
@Api(tags = {"MDS v1"})
@ApiService(value="MDS", major=1, minor=0)
public class MdsApi {

	private static Logger logger = Logger.getLogger(MdsApi.class);

	@GET
	@Path("/metadatasetsV2/{repository}")

	@ApiOperation(
			value = "Get metadata sets V2 of repository.",
			notes = "Get metadata sets V2 of repository.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "OK.", response = MdsEntriesV2.class),
					@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
					@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
					@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
					@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
					@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class)
			})

	public Response getMetadataSetsV2(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {

			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().getMetadataSetsV2(repository, req);
			}

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsEntriesV2 result=new MdsEntriesV2();
			result.setMetadatasets(MdsDaoV2.getAllMdsDesc(repoDao));

			return Response.status(Response.Status.OK).entity(result).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@GET
	@Path("/metadatasetsV2/{repository}/{metadataset}")

	@ApiOperation(
			value = "Get metadata set new.",
			notes = "Get metadata set new.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = MdsV2.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getMetadataSetV2(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
			@Context HttpServletRequest req) {

		try {

			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().getMetadataSetV2(repository, mdsId, req);
			}

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			return Response.status(Response.Status.OK).entity((MdsDaoV2.getMds(repoDao, mdsId).asMds())).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@OPTIONS
	@Path("/metadatasets/{repository}/{metadataset}")
	@ApiOperation(hidden = true, value = "")

	public Response options2() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}


	/**
	 *
	 */

	@POST
	@Path("/metadatasetsV2/{repository}/{metadataset}/values")

	@ApiOperation(
			value = "Get values.",
			notes = "Get values.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "OK.", response = Suggestions.class),
					@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
					@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
					@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
					@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
					@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class)
			})
	public Response getValuesV2(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
			@ApiParam(value = "suggestionParam") SuggestionParam suggestionParam,
			@Context HttpServletRequest req) {

		try {

			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().getValuesV2(repository, mdsId, suggestionParam, req);
			}

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDaoV2 mds = MdsDaoV2.getMds(repoDao, mdsId);
			Suggestions response = mds.getSuggestions(suggestionParam.getValueParameters().getQuery(),
					suggestionParam.getValueParameters().getProperty(),
					suggestionParam.getValueParameters().getPattern(),
					suggestionParam.getCriterias());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	/**
	 *
	 */

	@POST
	@Path("/metadatasetsV2/{repository}/{metadataset}/values_for_keys")

	@ApiOperation(
			value = "Get values for keys.",
			notes = "Get values for keys.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "OK.", response = Suggestions.class),
					@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
					@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
					@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
					@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
					@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class)
			})
	public Response getValues4KeysV2(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
			@ApiParam(value = "query") @QueryParam("query") String query,
			@ApiParam(value = "property") @QueryParam("property") String property,
			@ApiParam(value = "keys") ArrayList<String> keys,
			@Context HttpServletRequest req) {

		try {

			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				throw new Exception("not implemented yet");
				//return RepoProxyFactory.getRepoProxy().getValuesV2(repository, mdsId, suggestionParam, req);
			}

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDaoV2 mds = MdsDaoV2.getMds(repoDao, mdsId);

			Suggestions response = null;
			for(String key : keys){
				Suggestions rs = mds.getSuggestions(query,
						property,
						key,
						null);
				if(response == null) response = rs;
				else response.getValues().addAll(rs.getValues());
			}


			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
}
