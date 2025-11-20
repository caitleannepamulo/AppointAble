package com.example.appointable;

import java.util.Calendar;

public class Appointment {
    private String childName;
    private String service;
    private String time;
    private Calendar date;
    private String status;

    public Appointment(String childName, String service, String time,
                       Calendar date, String status) {
        this.childName = childName;
        this.service = service;
        this.time = time;
        this.date = date;
        this.status = status;
    }

    public String getChildName() { return childName; }
    public String getService() { return service; }
    public String getTime() { return time; }
    public Calendar getDate() { return date; }
    public String getStatus() { return status; }
}
