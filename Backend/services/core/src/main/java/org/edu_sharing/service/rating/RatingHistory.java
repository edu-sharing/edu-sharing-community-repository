package org.edu_sharing.service.rating;

public class RatingHistory extends RatingBase {
    private  String timestamp;

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public String getTimestamp(){
        return this.timestamp;
    }
}
