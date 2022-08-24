package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.ValuespaceData;
import org.edu_sharing.metadataset.v2.ValuespaceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ValuespaceReader {
    static Logger logger = Logger.getLogger(ValuespaceReader.class);
    protected final ValuespaceInfo info;
    public ValuespaceReader(ValuespaceInfo valuespaceInfo) {
        this.info = valuespaceInfo;
    }
    public static ValuespaceReader getSupportedReader(ValuespaceInfo valuespace){
        List<ValuespaceReader> readers = new ArrayList<>();

        readers.add(new OpenSALTReader(valuespace));
        readers.add(new CurriculumReader(valuespace));
        readers.add(new SKOSReader(valuespace));

        readers = readers.stream().filter(ValuespaceReader::supportsUrl).collect(Collectors.toList());

        if(readers.size() == 0){
            logger.warn("The given valuespace uri "+valuespace.getValue()+" can not be resolved for a supported provider. Consider to explicitly set a valuespace type.");
        } else if(readers.size() > 1){
            logger.warn("The given valuespace uri "+valuespace.getValue()+" matches multiple providers");
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
        Matcher matched = pattern.matcher(info.getValue());
        return matched;
    }

    public abstract ValuespaceData getValuespace(String locale) throws Exception;

    protected abstract boolean supportsUrl();
}
