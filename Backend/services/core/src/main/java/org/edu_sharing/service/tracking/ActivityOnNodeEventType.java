package org.edu_sharing.service.tracking;

public enum ActivityOnNodeEventType {
    DOWNLOAD_MATERIAL,
    VIEW_MATERIAL,
    OPEN_EXTERNAL_LINK, // open link in new tab was choosen
    VIEW_COLLECTION,
    VIEW_MATERIAL_EMBEDDED,
    VIEW_MATERIAL_PLAY_MEDIA, // When a video or audio file is actually started playing
}
