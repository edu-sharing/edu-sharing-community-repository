package org.edu_sharing.service.lifecycle;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonDeleteResult {
    public String authorityName;
    // how the person is displayed on deleted objects, e.g. comments, ratings, etc.
    public String deletedName;
    // map because of scoped or non scoped-enviorments
    public Map<String,Counts> homeFolder=new HashMap<>();
    public Map<String,Counts> sharedFolders=new HashMap<>();

    public CollectionCounts collections;
    public int comments,ratings,collectionFeedback,stream;
    public static class Counts {
        private List<Element> elements;

        public List<Element> getElements() {
            return elements;
        }

        public Counts(List<Element> elements){
            this.elements = elements;
        }
    }
    public static class CollectionCounts{
        private List<Element> refs;
        private List<Element> collections;

        public List<Element> getRefs() {
            return refs;
        }

        public void setRefs(List<Element> refs) {
            this.refs = refs;
        }

        public List<Element> getCollections() {
            return collections;
        }

        public void setCollections(List<Element> collections) {
            this.collections = collections;
        }
    }

    public static class Element {
        private String id;
        private String name;
        private String type;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Element(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}
