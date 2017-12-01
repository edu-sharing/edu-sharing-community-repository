package org.edu_sharing.service.suggest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.SQLKeyword;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class SuggestDAOSQLImpl implements SuggestDAO {

	MetadataSetBaseProperty property;
	
	String sqlStatement;
	
	Logger logger = Logger.getLogger(SuggestDAOSQLImpl.class);
	
	public SuggestDAOSQLImpl() {
	}
	
	@Override
	public String getValue(String key) {
		String result = null;;
		Connection con = null;
		PreparedStatement statement = null;
		
		try{
			con = ConnectionPool.getConnection();
			statement = con.prepareStatement(this.sqlStatement);
			
			key = StringEscapeUtils.escapeSql(key);
			key = key.toLowerCase();
			key = key.trim();
			statement.setString(1, key);
			ResultSet resultSet = statement.executeQuery();
			
			boolean test = resultSet.next();
			if (test){
				String kwValue = resultSet.getString(1);
				result = kwValue;
			}
		
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}finally {
			ConnectionPool.cleanUp(con, statement);
		}
		
		return result;
	}
	
	@Override
	public List<? extends Suggestion> query(String query) {
		
		List<SQLKeyword> result = new ArrayList<SQLKeyword>();
		Connection con = null;
		PreparedStatement statement = null;
		
		if(query == null || query.trim().equals("")){
			return result;
		}
		
		try{
			
			con = ConnectionPool.getConnection();
			statement = con.prepareStatement(this.sqlStatement);
			
			query = StringEscapeUtils.escapeSql(query);
			statement.setString(1,"%" + query.toLowerCase() + "%");
			
			ResultSet resultSet = statement.executeQuery();
		
			while(resultSet.next()){
				String kwValue = resultSet.getString(1);
				SQLKeyword sqlKw = new SQLKeyword();
				sqlKw.setKeyword(kwValue.trim());
				result.add(sqlKw);
			}
		
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}finally {
			ConnectionPool.cleanUp(con, statement);
		}
		
		return result;
	}

	@Override
	public void setMetadataProperty(MetadataSetBaseProperty property) {
		this.property = property;
		this.sqlStatement = this.property.getParam("statement");		
	}
	
}
