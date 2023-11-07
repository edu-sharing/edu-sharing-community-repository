package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.update.UpdateFactory;
import org.edu_sharing.repository.server.update.UpdateFactoryInfo;
import org.edu_sharing.repository.server.update.UpdateInfo;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SQLUpdater implements UpdateFactory {

    public static final String ID = "SQLUpdater_";

    @Override
    public List<UpdateInfo> getUpdates() {
        List<UpdateInfo> updateInfoList = new ArrayList<>();
        updateInfoList.addAll(getUpdates("repository.database.scripts.core"));
        updateInfoList.addAll(getUpdates("repository.database.scripts.custom"));
        return updateInfoList;
    }

    private List<UpdateInfo> getUpdates(String property) {
        try {
            List<String> sqlScripts = LightbendConfigLoader.get().getStringList(property);
            ArrayList<UpdateInfo> updateInfos = new ArrayList<>(sqlScripts.size());
            if (sqlScripts.size() > 0) {
                for (int i = 0; i < sqlScripts.size(); i++) {
                    String script = sqlScripts.get(i);
                    UpdateFactoryInfo updateInfo = new UpdateFactoryInfo(
                            ID + script,
                            "SQLUpdater to run sql scripts defined in the repository.database.scripts config",
                            true,
                            100000 + i,
                            true,
                            false,
                            (test) -> runSQLScript(script, test));
                    updateInfos.add(updateInfo);
                }
            } else {
                log.info("no scripts to execute defined in property " + property);
            }
            return updateInfos;
        } catch (Throwable e) {
            log.error("Error running one or more sql scripts defined in " + property, e);
            return new ArrayList<>();
        }
    }

    private void runSQLScript(String script, boolean test) {
        if(test) {
            return;
        }

        try {
            ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
            Connection connection = dbAlf.getConnection();
            connection.setAutoCommit(false);
            ScriptRunner scriptRunner = new ScriptRunner(connection);

            InputStream is = PropertiesHelper.Config.getInputStreamForFile(PropertiesHelper.Config.PATH_CONFIG + PropertiesHelper.Config.PathPrefix.DEFAULTS_DATABASE + "/" + script);

            scriptRunner.setSendFullScript(true);
            //scriptRunner.setEscapeProcessing(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.setErrorLogWriter(new PrintWriter(System.out));
            scriptRunner.runScript(new InputStreamReader(is, StandardCharsets.UTF_8));
            connection.commit();

            dbAlf.cleanUp(connection);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
