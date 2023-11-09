package org.edu_sharing.alfresco.metadata;

import org.alfresco.repo.content.MimetypeMap;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;
import org.edu_sharing.alfresco.apache.tika.parser.mp4.MP4Parser;
import org.gagravarr.tika.FlacParser;
import org.gagravarr.tika.VorbisParser;

//import org.alfresco.transformer.metadataExtractors.TikaAudioMetadataExtractor;

import java.util.ArrayList;

/**
 * TikaAudioMetadataExtracter
 * 100% cpu problem with some videos.
 * MP4Parser is fixed
 *
 * Uses same name as alfresco version to make
 * AbstractMappingMetadataExtracter.getDefaultMapping works
 */
public class TikaAudioMetadataExtracter //extends TikaAudioMetadataExtractor
{

    // The Audio related parsers we use
    //    private static Parser[] parsers = new Parser[] {
    //            new VorbisParser(),
    //            new FlacParser(),
    //            new MP4Parser()
    //    };
    //
    //    public TikaAudioMetadataExtracter()
    //    {
    //        super();
    //    }
    //
    //    @Override
    //    protected Parser getParser()
    //    {
    //        return new CompositeParser(
    //                tikaConfig.getMediaTypeRegistry(), parsers
    //        );
    //    }

}

