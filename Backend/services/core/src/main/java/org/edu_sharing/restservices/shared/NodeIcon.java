package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeIcon implements Serializable {
    @JsonPropertyDescription("Url to an icon to display")
    private String url;
    @JsonPropertyDescription("Optional font glyph id to display a font-based icon. Should be preferred if set")
    private String fontGlyphId;
}
