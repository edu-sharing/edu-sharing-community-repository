package org.edu_sharing.service.nodeservice;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.VCardConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * interceptor that generates an combined field virtual:all_authors for the frontend including all authors and author_freetext
 */
@Slf4j
public class PropertiesGetInterceptorAuthors extends PropertiesGetInterceptorDefault {
    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        try {
            List<String> result = getAllAuthors(context.getProperties());

            // fix after DESP-738
            context.getProperties().put("{virtualproperty}all_authors",
                    ValueTool.toMultivalue(result.toArray(String[]::new))
            );
        } catch(Throwable t) {
            log.warn(t.getMessage(), t);
        }
        return context.getProperties();
    }

    /**
     * all authors of a node: author freetext + display names of all author vcards
     * @param properties native node properties (global names)
     * @return list of author names, never null
     */
    public static List<String> getAllAuthors(Map<String, Object> properties) {
        Object author = properties.get(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
        if(author == null) {
            author = Collections.emptyList();
        }
        Object freetext = properties.get(CCConstants.CCM_PROP_AUTHOR_FREETEXT);
        if(freetext == null) {
            freetext = Collections.emptyList();
        }
        return Stream.concat(
                ((freetext instanceof List) ? ((List<String>)freetext) : Arrays.asList(ValueTool.getMultivalue(freetext.toString()))).stream(),
                ((author instanceof List) ? ((List<String>)author) : Arrays.asList(ValueTool.getMultivalue(author.toString()))).stream().map(VCardConverter::getNameForVCardString)
        ).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }
}
