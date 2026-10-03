package com.example.doubtbridge.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppConstants {
    public static final String[] DEPARTMENTS = {
        "All Departments", "Civil", "Computer Science", "Electrical",
        "Electronics", "Information Technology", "Mechanical Engineering", "Other"
    };

    public static final String[] CLASSES = {
        "All Classes", "FY", "SY", "TY", "Final Year"
    };

    public static final int MATCH_THRESHOLD = 30;

    /**
     * Returns a dynamic list of subjects based on Department and Class Year.
     */
    public static List<String> getSubjects(String dept, String year) {
        List<String> subjects = new ArrayList<>();

        // If "All Departments" is selected, return a broad list
        if (dept.equals("All Departments") || dept.equals("Other")) {
            return Arrays.asList("Math", "Physics", "Chemistry", "English", "Other");
        }

        // --- COMPUTER SCIENCE ---
        if (dept.equals("Computer Science")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Programming in C", "Engineering Maths I", "Digital Electronics", "Physics"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Data Structures", "Java Programming", "Discrete Mathematics", "Microprocessors"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Operating Systems", "Database Management", "Computer Networks", "Software Engineering"));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Machine Learning", "Cloud Computing", "Information Security", "Project Management"));
            }
        }
        // --- INFORMATION TECHNOLOGY ---
        else if (dept.equals("Information Technology")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Programming in C", "Engineering Maths I", "Basic Electronics", "Physics"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Data Structures", "Python Programming", "Discrete Structures", "Computer Organization"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Web Technologies", "Database Management", "Software Engineering", "Computer Networks"));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Cyber Security", "Big Data Analytics", "Cloud Computing", "Internet of Things"));
            }
        }
        // --- CIVIL ---
        else if (dept.equals("Civil")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Applied Mechanics", "Engineering Maths I", "Basic Civil Eng.", "Physics"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Surveying", "Building Construction", "Strength of Materials", "Fluid Mechanics"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Structural Analysis", "Concrete Technology", "Transportation Eng.", "Environmental Eng."));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Foundation Engineering", "Hydrology", "Construction Mgmt", "Bridge Engineering"));
            }
        }
        // --- ELECTRICAL ---
        else if (dept.equals("Electrical")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Basic Electrical Eng.", "Engineering Maths I", "Physics", "Engineering Graphics"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Network Analysis", "Electrical Machines I", "Electromagnetic Fields", "Electrical Measurements"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Control Systems", "Power Systems I", "Power Electronics", "Electrical Machines II"));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("High Voltage Engineering", "Renewable Energy", "Smart Grid", "Power Systems II"));
            }
        }
        // --- ELECTRONICS ---
        else if (dept.equals("Electronics")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Basic Electronics", "Engineering Maths I", "Physics", "Engineering Graphics"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Network Theory", "Analog Electronics", "Digital Electronics", "Signals & Systems"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Microprocessors", "Analog Communications", "VLSI Design", "Control Systems"));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Embedded Systems", "Digital Signal Processing", "Wireless Communication", "Optical Fiber Communication"));
            }
        }
        // --- MECHANICAL ---
        else if (dept.equals("Mechanical Engineering")) {
            if (year.equals("FY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Engineering Graphics", "Engineering Maths I", "Basic Mech. Eng.", "Chemistry"));
            }
            if (year.equals("SY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Thermodynamics", "Material Science", "Machine Drawing", "Theory of Machines"));
            }
            if (year.equals("TY") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("Heat Transfer", "Fluid Machinery", "Manufacturing Process", "Design of Machine Elements"));
            }
            if (year.equals("Final Year") || year.equals("All Classes")) {
                subjects.addAll(Arrays.asList("CAD/CAM", "Refrigeration & AC", "Mechatronics", "Automobile Engineering"));
            }
        }

        if (subjects.isEmpty()) subjects.add("Other");
        
        // Remove duplicates if "All Classes" added multiple similar subjects
        List<String> uniqueSubjects = new ArrayList<>();
        for (String s : subjects) {
            if (!uniqueSubjects.contains(s)) uniqueSubjects.add(s);
        }
        return uniqueSubjects;
    }
}
