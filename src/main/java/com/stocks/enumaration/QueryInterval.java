package com.stocks.enumaration;

public enum QueryInterval {
    DAILY("1d"),
    WEEKLY("5d"),
    MONTHLY("1mo"),
    FIFTEEN_MINS( "15m"),
    THREE_MINS("3m"),
    FIVE_MINS("5m");

    private final String tag;

    private QueryInterval(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return this.tag;
    }

    public static QueryInterval getInstance(String tag){
        for(QueryInterval queryInterval : values()){
            if(queryInterval.getTag().equals(tag)){
                return  queryInterval;
            }
        }
        return QueryInterval.FIVE_MINS;
    }
}