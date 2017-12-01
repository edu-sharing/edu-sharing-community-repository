package org.edu_sharing.restservices;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import javax.ws.rs.ApplicationPath;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath(value = "/rest")
public class ApiApplication extends ResourceConfig {

	public static final int MAJOR = 1;
	public static final int MINOR = 1;
	
	public static final Class<?>[] SERVICES = new Class<?>[] {
		org.edu_sharing.restservices.about.v1.AboutApi.class,
		org.edu_sharing.restservices.admin.v1.AdminApi.class,
		org.edu_sharing.restservices.collection.v1.CollectionApi.class,
		org.edu_sharing.restservices.config.v1.ConfigApi.class,
		org.edu_sharing.restservices.iam.v1.IamApi.class,
//		org.edu_sharing.restservices.knowledge.v1.KnowledgeApi.class,
		org.edu_sharing.restservices.login.v1.LoginApi.class,
		org.edu_sharing.restservices.mds.v1.MdsApi.class,
		org.edu_sharing.restservices.network.v1.NetworkApi.class,
		org.edu_sharing.restservices.node.v1.NodeApi.class,
		org.edu_sharing.restservices.organization.v1.OrganizationApi.class,
		org.edu_sharing.restservices.search.v1.SearchApi.class,
		org.edu_sharing.restservices.usage.v1.UsageApi.class,
		org.edu_sharing.restservices.rendering.v1.RenderingApi.class,
		org.edu_sharing.restservices.statistic.v1.StatisticApi.class,
		org.edu_sharing.restservices.archive.v1.ArchiveApi.class,
		org.edu_sharing.restservices.clientutils.v1.ClientUtilsApi.class,
		org.edu_sharing.restservices.tool.v1.ToolApi.class
	};
	
	public ApiApplication() {

		// multi-part feature

		this.register(MultiPartFeature.class);

		// custom services

		this.registerClasses(SERVICES);		
		this.packages(getClass().getPackage().getName());

		// swagger service

		this.register(ApiListingResource.class, SwaggerSerializers.class);
		this.packages("io.swagger.jaxrs.listing");

		final BeanConfig beanConfig = new BeanConfig();

		beanConfig.setTitle("edu-sharing Repository REST API");
		beanConfig.setDescription("The public restful API of the edu-sharing repository.");
		beanConfig.setVersion(MAJOR + "." + MINOR);
		beanConfig.setBasePath(
				"/" + ApplicationInfoList.getHomeRepository().getWebappname()
				+ getClass().getAnnotation(ApplicationPath.class).value());
		beanConfig.setResourcePackage(getClass().getPackage().getName());

		beanConfig.setScan(true);

	}

}