package com.comm.bean;

/**
 * Created by vipli on 2016/11/10 0010.
 */

public class Node {
    private String id;	    //设备ID
    private float tep;		//温度
    private float noice;	//噪音
    private long date;		//时间戳

    public Node() {};

    public Node(String id, float tep, float noice, long date) {
        this.id = id;
        this.tep = tep;
        this.noice = noice;
        this.date = date;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getTep() {
        return tep;
    }

    public void setTep(float tep) {
        this.tep = tep;
    }

    public float getNoice() {
        return noice;
    }

    public void setNoice(float noice) {
        this.noice = noice;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", tep='" + tep + '\'' +
                ", noice='" + noice + '\'' +
                ", date=" + date +
                '}';
    }
}
