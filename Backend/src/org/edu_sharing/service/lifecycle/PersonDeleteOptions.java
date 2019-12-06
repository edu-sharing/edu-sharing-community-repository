package org.edu_sharing.service.lifecycle;

import java.io.Serializable;

public class PersonDeleteOptions implements Serializable {
    public enum DeleteMode {
        none,assign,delete
    }
    public enum FolderDeleteMode {
        none,assign
    }
    public boolean cleanupMetadata;
    public HomeFolderOptions homeFolder;
    public SharedFolderOptions sharedFolders;
    public CollectionOptions collections;
    public DeleteOption ratings,comments,collectionFeedback,statistics,stream;
    public String receiver,receiverGroup;

    public static class FolderOptions {
        public FolderDeleteMode folders;
        public DeleteMode privateFiles,ccFiles;
    }
    public static class HomeFolderOptions extends FolderOptions{
        public boolean keepFolderStructure;
    }
    public static class SharedFolderOptions extends FolderOptions{
        public boolean move;
    }

    public static class CollectionOptions {
        public DeleteMode privateCollections,publicCollections;
    }

    public static class DeleteOption {
        public boolean delete;
    }
}
