package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Right sidebar preview mode: show sidebar with optional preview, or jump directly to render page")
public enum SearchPreviewMode {
    @Schema(description = "Show sidebar with preview (if RS2 active)")
    Sidebar,
    @Schema(description = "Direct jump to rendering page (like v9.1)")
    RenderingPage
}
