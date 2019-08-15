package org.edu_sharing.service.rating;

import java.util.HashMap;

public class AccumulatedRatings {
    private RatingData overall;
    private double user;
    private HashMap<Object, RatingData> affiliation;

    public RatingData getOverall() {
        return overall;
    }

    public void setOverall(RatingData overall) {
        this.overall = overall;
    }

    public double getUser() {
        return user;
    }

    public void setUser(double user) {
        this.user = user;
    }

    public void setAffiliation(HashMap<Object, RatingData> affiliation) {
        this.affiliation = affiliation;
    }

    public HashMap<Object, RatingData> getAffiliation() {
        return affiliation;
    }

    public static class RatingData{
        private double sum;
        private long count;

        public RatingData(double sum, long count) {
            this.sum=sum;
            this.count=count;
        }

        public double getRating(){
            if(count==0)
                return 0;
            return (double)sum/count;
        }
        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getSum() {
            return sum;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }
    }
}
