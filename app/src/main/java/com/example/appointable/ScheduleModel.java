package com.example.appointable;

public class ScheduleModel {

    private String childName;
    private String service;
    private String time;
    private int sortTimeMinutes;
    private int status = 0;
    private int dayOfWeek;

    public ScheduleModel(String childName,
                         String service,
                         String time,
                         int sortTimeMinutes,
                         int dayOfWeek) {
        this.childName = childName;
        this.service = service;
        this.time = time;
        this.sortTimeMinutes = sortTimeMinutes;
        this.dayOfWeek = dayOfWeek;
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

    public int getDayOfWeek() {
        return dayOfWeek;
    }
}
