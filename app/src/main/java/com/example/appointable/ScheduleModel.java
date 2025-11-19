package com.example.appointable;

public class ScheduleModel {

    private String childName;
    private String service;
    private String time;
    private int sortTimeMinutes;
    private int status = 0;

    public ScheduleModel(String childName, String service, String time, int sortTimeMinutes) {
        this.childName = childName;
        this.service = service;
        this.time = time;
        this.sortTimeMinutes = sortTimeMinutes;
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

    public int getSortTimeMinutes() {
        return sortTimeMinutes;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
