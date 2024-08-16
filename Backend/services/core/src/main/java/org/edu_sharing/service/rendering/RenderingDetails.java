package org.edu_sharing.service.rendering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.edu_sharing.restservices.NodeDao;

@Data
@AllArgsConstructor
public class RenderingDetails {
    String details;
    RenderingServiceData renderingServiceData;
}
