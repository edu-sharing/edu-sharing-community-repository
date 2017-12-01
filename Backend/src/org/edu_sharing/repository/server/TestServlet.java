package org.edu_sharing.repository.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.tools.metadata.search.SearchMetadataHelper;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class TestServlet extends HttpServlet {
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//&user=admin&password=admin#
		String userName = req.getParameter("user");
		String password = req.getParameter("password");
		
		String metadataSetId = req.getParameter("mds");
		metadataSetId = (metadataSetId == null)? "default" : metadataSetId;
		
		String queryProperty = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_publisher";
		
		try{
			
			String homeAppId = ApplicationInfoList.getHomeRepository().getAppId();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeAppId);
			
			HashMap<String,String> authInfo = authTool.createNewSession(userName, password);
			
			MCAlfrescoBaseClient baseclient = (MCAlfrescoBaseClient)RepoFactory.getInstance(homeAppId,authInfo );
			
			MetadataSets mdss = RepoFactory.getMetadataSetsForRepository(homeAppId);
			MetadataSet mds = mdss.getMetadataSetById(metadataSetId);
			List<MetadataSetQuery> queries = mds.getMetadataSetQueries().getMetadataSetQueries();
			
			MetadataSetQueryProperty prop = null;
			for(MetadataSetQuery query:queries){
				for(MetadataSetQueryProperty property : query.getProperties() ){
					
					
					System.out.println("property.getName():"+property.getName());
					if(property.getName().equals(queryProperty)){
						
						prop = property;
						
						
					}
				}
			}
			
			if(prop != null){
				
				for(MetadataSetValueKatalog vsEntry : prop.getValuespace()){
					SearchMetadataHelper smdh = new SearchMetadataHelper();
					
					String value = vsEntry.getKey();
					SearchCriterias sc = new SearchCriterias();
						
					sc.setMetadataSetId(metadataSetId);
					sc.setMetadataSetSearchData(smdh.createSearchData(prop, new String[]{value}));
					SearchToken st = new SearchToken();
					st.setSearchCriterias(sc);
					SearchResult sr = baseclient.search(st);
					
					int nodeCount = sr.getNodeCount();
					
					System.out.println("Found "+nodeCount+" for "+value);
					
					if(nodeCount < 1){
						resp.getOutputStream().println("found nothing for:"+value);
					}
					
				}
			}else{
				System.out.println("could not find a query property for:"+queryProperty);
			}
			
			
		}catch(Throwable e){
			e.printStackTrace();
			resp.getOutputStream().println(e.getMessage());
		}
		
	}

}
