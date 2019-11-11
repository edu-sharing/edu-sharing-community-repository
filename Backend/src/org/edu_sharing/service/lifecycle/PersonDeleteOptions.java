package org.edu_sharing.service.lifecycle;

import java.io.Serializable;

public class PersonDeleteOptions implements Serializable {
    public enum DeleteMode {
        none,assign,delete
    }

    public HomeFolderOptions homeFolder;
    public FolderOptions sharedFolders;
    public CollectionOptions collections;
    public DeleteOption ratings,comments,statistics,stream;
    public String receiver,receiverGroup;

    public static class FolderOptions {
        public DeleteMode privateFiles,ccFiles;
    }
    public static class HomeFolderOptions extends FolderOptions{
        public boolean keepFolderStructure;
    }


    public static class CollectionOptions {
        public DeleteMode privateCollections,publicCollections;
    }

    public static class DeleteOption {
        public boolean delete;
    }
}
