package org.edu_sharing.restservices;

import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;

import javax.ws.rs.ApplicationPath;


import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@ApplicationPath(value = "/rest")
public class ApiApplication extends ResourceConfig {

	public static final int MAJOR = 1;
	public static final int MINOR = 1;
	
	public static final Class<?>[] SERVICES = new Class<?>[] {
		org.edu_sharing.restservices.about.v1.AboutApi.class,
		org.edu_sharing.restservices.admin.v1.AdminApi.class,
		org.edu_sharing.restservices.bulk.v1.BulkApi.class,
		org.edu_sharing.restservices.collection.v1.CollectionApi.class,
		org.edu_sharing.restservices.comment.v1.CommentApi.class,
		org.edu_sharing.restservices.rating.v1.RatingApi.class,
		org.edu_sharing.restservices.config.v1.ConfigApi.class,
		org.edu_sharing.restservices.iam.v1.IamApi.class,
//		org.edu_sharing.restservices.knowledge.v1.KnowledgeApi.class,
		org.edu_sharing.restservices.login.v1.LoginApi.class,
		org.edu_sharing.restservices.mds.v1.MdsApi.class,
		org.edu_sharing.restservices.mediacenter.v1.MediacenterApi.class,
		org.edu_sharing.restservices.network.v1.NetworkApi.class,
		org.edu_sharing.restservices.node.v1.NodeApi.class,
		org.edu_sharing.restservices.organization.v1.OrganizationApi.class,
		org.edu_sharing.restservices.search.v1.SearchApi.class,
		org.edu_sharing.restservices.usage.v1.UsageApi.class,
		org.edu_sharing.restservices.rendering.v1.RenderingApi.class,
		org.edu_sharing.restservices.statistic.v1.StatisticApi.class,
		org.edu_sharing.restservices.tracking.v1.TrackingApi.class,
		org.edu_sharing.restservices.archive.v1.ArchiveApi.class,
		org.edu_sharing.restservices.clientutils.v1.ClientUtilsApi.class,
		org.edu_sharing.restservices.tool.v1.ToolApi.class,
		org.edu_sharing.restservices.register.v1.RegisterApi.class,
		org.edu_sharing.restservices.sharing.v1.SharingApi.class,
		org.edu_sharing.restservices.lti.v13.LTIApi.class,
		org.edu_sharing.restservices.ltiplatform.v13.LTIPlatformApi.class
	};
	
	public ApiApplication() {

		// multi-part feature

		this.register(MultiPartFeature.class);

		// custom services

		this.registerClasses(SERVICES);		
		this.packages(getClass().getPackage().getName());

		// swagger service
		/**
		 * @TODO: ApiListingResource
		 */
		//this.register(ApiListingResource.class, SwaggerSerializers.class);
		this.register(SwaggerSerializers.class);
		//this.packages("io.swagger.jaxrs.listing");
		this.packages("io.swagger.v3.jaxrs2.integration.resources");



		//final BeanConfig beanConfig = new BeanConfig();

		OpenAPI oas = new OpenAPI();
		Info info = new Info();
		info.setTitle("edu-sharing Repository REST API");
		info.setDescription("The public restful API of the edu-sharing repository.");
		info.setVersion(MAJOR + "." + MINOR);
		oas.info(info);
		String url = "/" + ApplicationInfoList.getHomeRepository().getWebappname() + "/rest";
				//+ getClass().getAnnotation(ApplicationPath.class).value();
		oas.servers(Collections.singletonList(new Server().url(url)));

		SwaggerConfiguration oasConfig = new SwaggerConfiguration()
				.openAPI(oas)
				.prettyPrint(true)
				.resourcePackages(Stream.of(getClass().getPackage().getName()).collect(Collectors.toSet()));

		/**
		 * @TODO
		 */
		//beanConfig.setScan(true);



		try {
			new JaxrsOpenApiContextBuilder()
					/**
					 * @TODO
					 */
					//.servletConfig(servletConfig)
					.application(this)
					.openApiConfiguration(oasConfig)
					.buildContext(true);
		} catch (OpenApiConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}


	}

}