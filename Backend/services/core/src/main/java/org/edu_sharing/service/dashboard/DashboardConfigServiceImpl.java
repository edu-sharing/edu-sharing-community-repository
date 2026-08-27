package org.edu_sharing.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.config.model.ConfigVisibility;
import org.edu_sharing.alfresco.service.config.model.ShortcutConfig;
import org.edu_sharing.alfresco.service.config.model.ShortcutConfigEntry;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.dashboard.models.DashboardShortcut;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.util.CheckedCast;
import org.edu_sharing.util.CheckedFunction;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Service
@RequiredArgsConstructor
public class DashboardConfigServiceImpl implements DashboardConfigService {

    private final PersonService personService;
    private final NodeService nodeService;
    private final ToolPermissionService toolPermissionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @NotNull
    private static ShortcutConfig getShortcutConfig() {
        try {
            return ConfigServiceFactory.getCurrentConfig().values.frontpage.dashboard.shortcuts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Cacheable(value = "dashboardShortcuts", key = "#username")
    @Override
    public List<DashboardShortcut> getDashboardShortcuts(String username) {
        NodeRef person = personService.getPersonOrNull(username);
        if (person == null) {
            return getDefaultDashBoardShortCuts();
        }
        Optional<List<String>> shortcutConfigs = Optional.ofNullable(nodeService.getProperty(person, QName.createQName(CCConstants.CCM_PROP_PERSON_DASHBOARD_SHORTCUT_CONFIG)))
                .map(CheckedCast.wrapToListOf(String.class));

        return shortcutConfigs
                .map(strings -> strings.stream()
                        .map(CheckedFunction.wrapOrThrow(x -> objectMapper.readValue(x, DashboardShortcut.class), IllegalStateException::new))
                        .collect(Collectors.toList()))
                .orElseGet(this::getDefaultDashBoardShortCuts);

    }

    @CacheEvict(value = "dashboardShortcuts", key = "#username")
    @Override
    public void setDashboardShortcuts(String username, List<DashboardShortcut> shortcuts) {
        NodeRef person = personService.getPersonOrNull(username);
        if (person == null) {
            return;
        }

        if (shortcuts == null) {
            nodeService.removeProperty(person, QName.createQName(CCConstants.CCM_PROP_PERSON_DASHBOARD_SHORTCUT_CONFIG));
        } else {

            List<ShortcutConfigEntry> shortcutConfigEntries = getShortcutConfig().entries;

            for (int i = 0; i < shortcuts.size(); i++) {
                DashboardShortcut shortcut = shortcuts.get(i);
                if (shortcut instanceof DashboardShortcut.RefDashboardShortcut) {
                    DashboardShortcut.RefDashboardShortcut refShortcut = (DashboardShortcut.RefDashboardShortcut) shortcut;
                    if (refShortcut.getRef() == null) {
                        throw new IllegalArgumentException("Ref must not be null of element " + i + " in shortcuts array.");
                    }
                    if (!nodeService.exists(new NodeRef(refShortcut.getRef()))) {
                        throw new IllegalArgumentException("Ref node does not exist of element " + i + " in shortcuts array.");
                    }
                } else if (shortcut instanceof DashboardShortcut.DefaultDashboardShortcut) {
                    DashboardShortcut.DefaultDashboardShortcut defaultShortcut = (DashboardShortcut.DefaultDashboardShortcut) shortcut;
                    int index = i;
                    ShortcutConfigEntry shortcutConfigEntry = shortcutConfigEntries.stream()
                            .filter(x -> x.id.equals(defaultShortcut.getId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Default shortcut does not exist of element " + index + " in shortcuts array."));

                    if (StringUtils.isNotBlank(shortcutConfigEntry.toolPermission) && !toolPermissionService.hasToolPermission(shortcutConfigEntry.toolPermission)) {
                        throw new IllegalArgumentException("Default shortcut does not have required tool permission of element " + index + " in shortcuts array.");
                    }
                }
            }

            ArrayList<String> shortcutConfigs = shortcuts.stream()
                    .map(CheckedFunction.wrapOrThrow(objectMapper::writeValueAsString, IllegalArgumentException::new))
                    .collect(Collectors.toCollection(ArrayList::new));
            nodeService.setProperty(person, QName.createQName(CCConstants.CCM_PROP_PERSON_DASHBOARD_SHORTCUT_CONFIG), shortcutConfigs);

        }
    }

    @Override
    public List<DashboardShortcut> getDefaultDashBoardShortCuts() {

        ShortcutConfig shortcutConfig = getShortcutConfig();

        return shortcutConfig.entries.stream()
                .filter(x -> StringUtils.isBlank(x.toolPermission) || toolPermissionService.hasToolPermission(x.toolPermission))
                .filter(x -> x.defaultVisibility == ConfigVisibility.VISIBLE)
                .limit(shortcutConfig.maxEntries)
                .map(x -> new DashboardShortcut.DefaultDashboardShortcut(x.id, null))
                .collect(Collectors.toList());
    }

}
