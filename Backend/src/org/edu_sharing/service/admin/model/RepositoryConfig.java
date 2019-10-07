package org.edu_sharing.service.admin.model;

import java.io.Serializable;

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
        public enum Timespan{
            days_30,
            days_100,
            all
        };
        public int totalCount=50;
        public int displayCount=12;
        public Mode mode=Mode.rating;
        public Timespan timespan=Timespan.all;
        public String query;
        // the id of the collection, if mode == collection
        public String collection;

        public Frontpage(){}
    }
}
