package org.edu_sharing.service.admin.model;

import java.io.Serializable;
import java.util.List;

public class RepositoryConfig implements Serializable {
    public Frontpage frontpage = new Frontpage();

    public RepositoryConfig(){}

    public static class Frontpage implements Serializable{
        public enum Mode{
            collection,
            rating,
            views,
            downloads
        };

        public int totalCount=50;
        public int displayCount=12;
        public Mode mode=Mode.rating;
        public int timespan=30;
        public boolean timespanAll = false;
        public List<Query> queries;
        // the id of the collection, if mode == collection
        public String collection;

        public Frontpage(){}

        public static class Query {
            public Condition condition=new Condition();
            public String query;
        }
    }
    public static class Condition{
        public enum Type{
            TOOLPERMISSION
        }
        public Type type;
        public boolean negate;
        public String value;
    }
}
