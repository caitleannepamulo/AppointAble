package com.example.appointable;

public class StudentModel {

    public String studentNumber;
    public String firstName;
    public String lastName;
    public String middleName;
    public String suffix;
    public String grade;
    public String status;

    public StudentModel(String studentNumber,
                        String firstName,
                        String lastName,
                        String middleName,
                        String suffix,
                        String grade,
                        String status) {
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.suffix = suffix;
        this.grade = grade;
        this.status = status;
    }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }
}
