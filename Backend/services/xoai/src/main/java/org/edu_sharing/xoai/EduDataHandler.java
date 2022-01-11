package org.edu_sharing.xoai;

import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public interface EduDataHandler {
    default List<String> getSets() throws OAIException {
        return Collections.singletonList("default");
    };

    /**
     * fetch identifier list for a given range
     * @param offset start offset
     * @param length amount of items to fetch
     * @param set set name (might be null if no set is requested)
     * @return
     */
    ListItemIdentifiersResult getIdentifiers(int offset, int length, String set) throws OAIException;

    EduItem getItem(String identifier) throws IdDoesNotExistException, OAIException;


    ListItemIdentifiersResult getIdentifiersFrom(int var1, int var2, Date var3, String set) throws OAIException;

    ListItemIdentifiersResult getIdentifiersUntil(int var1, int var2, Date var3, String set) throws OAIException;

    ListItemIdentifiersResult getIdentifiersFromUntil(int var1, int var2, Date var3, Date var4, String set) throws OAIException;

}
