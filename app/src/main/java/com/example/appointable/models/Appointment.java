package com.example.appointable.models;

public class Appointment {

    private String id;
    private String studentId;
    private String childName;
    private String teacherId;
    private String teacherName;
    private String service;
    private String date;
    private String time;
    private String status;

    public Appointment() {}

    public Appointment(String id, String studentId, String childName,
                       String teacherId, String teacherName,
                       String service, String date, String time,
                       String status) {

        this.id = id;
        this.studentId = studentId;
        this.childName = childName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getChildName() { return childName; }
    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getService() { return service; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
}
