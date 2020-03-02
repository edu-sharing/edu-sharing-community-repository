package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;

import java.util.ArrayList;
import java.util.List;

public abstract class ValuespaceReader {
    static Logger logger = Logger.getLogger(ValuespaceReader.class);
    public static ValuespaceReader getSupportedReader(String valuespaceUrl){
        List<ValuespaceReader> readers = new ArrayList<>();

        OpenSALTReader openSALTReader = new OpenSALTReader(valuespaceUrl);
        if(openSALTReader.supportsUrl()) {
            readers.add(openSALTReader);
        }

        if(readers.size() == 0){
            logger.warn("The given valuespace uri "+valuespaceUrl+" can not be resolved for a supported provider");
        } else if(readers.size() > 1){
            logger.warn("The given valuespace uri "+valuespaceUrl+" matches multiple providers");
            for(ValuespaceReader reader : readers){
                logger.warn(reader.getClass().getName()+" matched");
            }
            logger.warn("Please check the url to make sure exatly one provider matches");
        } else {
            return readers.get(0);
        }
        return null;
    }

    public abstract List<MetadataKey> getValuespace() throws Exception;

    protected abstract boolean supportsUrl();
}
