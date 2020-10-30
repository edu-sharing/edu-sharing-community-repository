package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ValuespaceReader {
    static Logger logger = Logger.getLogger(ValuespaceReader.class);
    protected final String valuespaceUrl;

    public ValuespaceReader(String valuespaceUrl) {
        this.valuespaceUrl = valuespaceUrl;
    }

    public static ValuespaceReader getSupportedReader(String valuespaceUrl){
        List<ValuespaceReader> readers = new ArrayList<>();

        readers.add(new OpenSALTReader(valuespaceUrl));
        readers.add(new CurriculumReader(valuespaceUrl));
        readers.add(new SKOSReader(valuespaceUrl));

        readers = readers.stream().filter(ValuespaceReader::supportsUrl).collect(Collectors.toList());

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

    public Matcher matches(String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matched = pattern.matcher(valuespaceUrl);
        return matched;
    }

    public abstract List<MetadataKey> getValuespace(String locale) throws Exception;

    protected abstract boolean supportsUrl();
}
