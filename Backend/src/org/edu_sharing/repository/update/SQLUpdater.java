package org.edu_sharing.repository.update;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.context.ApplicationContext;

public class SQLUpdater extends UpdateAbstract {
	
	Logger logger = Logger.getLogger(SQLUpdater.class);
	
	public static final String ID = "SQLUpdater";
	
	public static final String description = "SQLUpdater to run sql scripts defined in the repository.database.scripts config";
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	SqlSessionFactory sqlSessionFactoryBean = (SqlSessionFactory)applicationContext.getBean("repoSqlSessionFactory");
	
	Protocol protocol = new Protocol();
	
	@Override
	public void execute() {
		execute(false);
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return ID;
	}
	
	@Override
	public void test() {
		execute(true);
	}
	
	private void execute(boolean test){
        executeScript("repository.database.scripts.core",test);
        executeScript("repository.database.scripts.custom",test);
    }

    private void executeScript(String property, boolean test) {
        try {
			List<String> sqlScripts = LightbendConfigLoader.get().getStringList(property);
			if(sqlScripts != null && sqlScripts.size() > 0) {
				for(String script : sqlScripts) {

					String sysProtocolEntry = ID + "_" + script;
					HashMap<String, Object> entry = protocol.getSysUpdateEntry(sysProtocolEntry);
					if(entry == null && !test) {
						logger.info("running sql script " + script);
						runSQLScript(script);
						protocol.writeSysUpdateEntry(sysProtocolEntry);
					}
				}
			}else {
				logger.info("no scripts to execute defined in property "+property);
			}
        }catch(Throwable e) {
            logger.error("Error running one or more sql scripts defined in "+property, e);
        }
    }

    private void runSQLScript(String script) throws Exception{
		
		ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
		Connection connection = dbAlf.getConnection();
		connection.setAutoCommit(false);
		ScriptRunner scriptRunner = new ScriptRunner(connection);
		
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(script);
		scriptRunner.setSendFullScript(true);
		//scriptRunner.setEscapeProcessing(true);
		scriptRunner.setStopOnError(true);
		scriptRunner.setErrorLogWriter(new PrintWriter(System.out));
		scriptRunner.runScript(new InputStreamReader(is, StandardCharsets.UTF_8));
		connection.commit();
	
		dbAlf.cleanUp(connection);
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}

}
