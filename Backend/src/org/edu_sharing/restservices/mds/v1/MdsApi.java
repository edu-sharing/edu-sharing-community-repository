package org.edu_sharing.restservices.mds.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.MdsDaoV2;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.mds.v1.model.MdsEntriesV2;
import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.mds.v1.model.Suggestions;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.MdsV2;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("/mds/v1")
@Tag(name="MDS v1")
@ApiService(value="MDS", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class MdsApi {

	private static Logger logger = Logger.getLogger(MdsApi.class);

	@GET
	@Path("/metadatasetsV2/{repository}")

	@Operation(summary = "Get metadata sets V2 of repository.", description = "Get metadata sets V2 of repository.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = MdsEntriesV2.class))),
					@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getMetadataSetsV2(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
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

	@Operation(summary = "Get metadata set new.", description = "Get metadata set new.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = MdsV2.class))),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})

	public Response getMetadataSetV2(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-" )) @PathParam("metadataset") String mdsId,
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
	@Hidden

	public Response options2() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}


	/**
	 *
	 */

	@POST
	@Path("/metadatasetsV2/{repository}/{metadataset}/values")

	@Operation(summary = "Get values.", description = "Get values.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Suggestions.class))),
					@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response getValuesV2(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-" )) @PathParam("metadataset") String mdsId,
			@Parameter(description = "suggestionParam") SuggestionParam suggestionParam,
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

	@Operation(summary = "Get values for keys.", description = "Get values for keys.")

	@ApiResponses(
			value = {
					@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = Suggestions.class))),
					@ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
			})
	public Response getValues4KeysV2(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, schema = @Schema(defaultValue="-default-" )) @PathParam("metadataset") String mdsId,
			@Parameter(description = "query") @QueryParam("query") String query,
			@Parameter(description = "property") @QueryParam("property") String property,
			@Parameter(description = "keys") ArrayList<String> keys,
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
