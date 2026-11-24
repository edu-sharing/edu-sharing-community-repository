package org.edu_sharing.repository.server.jobs.quartz;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobHandlerJobEntriesTest {

    private static final String DEFAULTS = "jobs.defaultEntries { "
            + "statusReport { name: \"Status Report Job\", class: \"a.StatusReportJob\", trigger: \"Cron[0 0 */4 * * ?]\" }, "
            + "other { name: \"Other Job\", class: \"a.OtherJob\", trigger: \"Daily\" } }";

    // defaults are ordered by their key ("other" < "statusReport"), followed by the entries
    @Test
    void defaultsAndEntriesAreMerged() {
        Config config = ConfigFactory.parseString(DEFAULTS + "\njobs.entries: [ { name: \"Custom Job\", class: \"a.CustomJob\", trigger: \"Daily\" } ]");
        assertEquals(List.of("Other Job", "Status Report Job", "Custom Job"), names(JobHandler.getJobEntries(config)));
    }

    @Test
    void defaultsSurviveReplacedEntries() {
        // projects replace the whole list, the defaults must still be scheduled
        Config config = ConfigFactory.parseString(DEFAULTS + "\njobs.entries: [ { name: \"Custom Job\", class: \"a.CustomJob\", trigger: \"Daily\" } ]")
                .withFallback(ConfigFactory.parseString("jobs.entries: [ { name: \"Reference Job\", class: \"a.RefJob\", trigger: \"Daily\" } ]"));
        assertEquals(List.of("Other Job", "Status Report Job", "Custom Job"), names(JobHandler.getJobEntries(config)));
    }

    @Test
    void defaultCanBeChangedAndDisabled() {
        Config config = ConfigFactory.parseString("jobs.defaultEntries.statusReport.trigger: \"Cron[0 0 */2 * * ?]\"\njobs.defaultEntries.other.enabled: false")
                .withFallback(ConfigFactory.parseString(DEFAULTS));
        List<Config> entries = JobHandler.getJobEntries(config);
        assertEquals(List.of("Status Report Job"), names(entries));
        assertEquals("Cron[0 0 */2 * * ?]", entries.get(0).getString("trigger"));
    }

    @Test
    void entryWithSameNameReplacesDefault() {
        Config config = ConfigFactory.parseString(DEFAULTS + "\njobs.entries: [ { name: \"Status Report Job\", class: \"a.StatusReportJob\", trigger: \"Daily[3,15]\" } ]");
        List<Config> entries = JobHandler.getJobEntries(config);
        assertEquals(List.of("Other Job", "Status Report Job"), names(entries));
        assertEquals("Daily[3,15]", entries.get(1).getString("trigger"));
    }

    @Test
    void onlyEntries() {
        Config config = ConfigFactory.parseString("jobs.entries: [ { name: \"Custom Job\", class: \"a.CustomJob\", trigger: \"Daily\" } ]");
        assertEquals(List.of("Custom Job"), names(JobHandler.getJobEntries(config)));
        assertEquals(List.of(), names(JobHandler.getJobEntries(ConfigFactory.empty())));
    }

    private static List<String> names(List<Config> entries) {
        return entries.stream().map(c -> c.getString("name")).collect(Collectors.toList());
    }
}
