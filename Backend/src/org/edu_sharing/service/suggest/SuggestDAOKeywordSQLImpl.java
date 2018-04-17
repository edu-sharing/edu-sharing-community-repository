package org.edu_sharing.service.suggest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.GNDKeywordDTO;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;

import com.google.gwt.user.client.ui.SuggestOracle;

public class SuggestDAOKeywordSQLImpl implements SuggestDAO {

	Logger logger = Logger.getLogger(SuggestDAOKeywordSQLImpl.class);
	
	String select = "select * from EDU_KEYWORD where lower(keyword_value) like ? limit 20";
	
	String selectOne = "select * from EDU_KEYWORD where keyword_ident = ?";
	
	@Override
	public List<? extends  SuggestOracle.Suggestion> query(String query) {
		
		Connection con = null;
		PreparedStatement statement = null;
		
		ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
		try {
			
			List<GNDKeywordDTO> result = new ArrayList<GNDKeywordDTO>();
			con = dbAlf.getConnection();
			
			query = StringEscapeUtils.escapeSql(query);
			statement = con.prepareStatement(select);
			statement.setString(1, query.toLowerCase()+"%");
			
			
			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next()){
				String kwIdent = resultSet.getString(resultSet.findColumn("KEYWORD_IDENT"));
				String kwValue = resultSet.getString(resultSet.findColumn("KEYWORD_VALUE"));
				String kwCatId = resultSet.getString(resultSet.findColumn("KEYWORD_CATEGORY_ID"));
				String keCat = resultSet.getString(resultSet.findColumn("KEYWORD_CATEGORY_VALUE"));
				
				GNDKeywordDTO kwdto = new GNDKeywordDTO();
				kwdto.setCategory(keCat);
				kwdto.setCategoryId(kwCatId);
				kwdto.setId(kwIdent);
				kwdto.setValue(kwValue);
				
				result.add(kwdto);
			}
			
			return result;
		
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			dbAlf.cleanUp(con, statement);
		}
		
		return null;
	}
	
	@Override
	public String getValue(String key) {
		Connection con = null;
		PreparedStatement statement = null;
		
		ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
		try {
			con = dbAlf.getConnection();
			statement = con.prepareStatement(selectOne);
			
			key =  StringEscapeUtils.escapeSql(key);
			statement.setString(1, key);
					
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()){
				String kwValue = resultSet.getString(resultSet.findColumn("KEYWORD_VALUE"));
				String cat = resultSet.getString(resultSet.findColumn("KEYWORD_CATEGORY_VALUE")).replaceAll(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR), " ; ");
				kwValue +="<span style=\"font-size:10px\"> ("+cat+")<span>";
				return kwValue;
			}
		
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
		} finally {
			dbAlf.cleanUp(con, statement);
		}
		
		return null;
	}
	
	public String getValueNoCat(String key) {
		Connection con = null;
		PreparedStatement statement = null;
		
		ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
		
		try{
			con = dbAlf.getConnection();
			statement = con.prepareStatement(selectOne);
			key= StringEscapeUtils.escapeSql(key);
			statement.setString(1, key);
					
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()){
				String kwValue = resultSet.getString(resultSet.findColumn("KEYWORD_VALUE"));
				return kwValue;
			}
		
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally {
			dbAlf.cleanUp(con, statement);
		}
		return null;
	}
	
	@Override
	public void setMetadataProperty(MetadataSetBaseProperty property) {
	}
	
}
