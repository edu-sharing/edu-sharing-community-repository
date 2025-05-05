package org.edu_sharing.metadataset.v2;

import lombok.Data;

import java.io.Serializable;

@Data
public class MetadataWidgetDefaults implements Serializable{
	private MetadataWidget.TextEscapingPolicy textEscapingPolicy;
}
