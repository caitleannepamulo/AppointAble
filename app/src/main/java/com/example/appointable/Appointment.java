package com.example.appointable;

public class Appointment {

    private String childName;
    private String service;
    private String time;
    private String status;
    private long dateMillis;

    public Appointment() {
    }

    public Appointment(String childName, String service, String time, String status, long dateMillis) {
        this.childName = childName;
        this.service = service;
        this.time = time;
        this.status = status;
        this.dateMillis = dateMillis;
    }

    public String getChildName() {
        return childName;
    }

    public String getService() {
        return service;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public long getDateMillis() {
        return dateMillis;
    }
}
