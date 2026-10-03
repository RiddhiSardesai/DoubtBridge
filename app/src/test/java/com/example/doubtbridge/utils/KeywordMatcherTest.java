package com.example.doubtbridge.utils;

import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.models.Note;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class KeywordMatcherTest {

    @Test
    public void testDefinitionMatching() {
        Doubt doubt = new Doubt("What is Software Testing?", "Please define software testing.", "student", "123", "CS", "Eng", "FY");
        
        Note note = new Note();
        note.setTitle("Introduction to Testing");
        note.setSubject("CS");
        // Slide 1 is TOC, Slide 2 is Definition
        note.setContent("Table of Contents\n1. Introduction\n2. Definition of Testing\n---SLIDE---\nSoftware Testing is a process of executing a program with the intent of finding errors.\n---SLIDE---\nTesting types include unit testing and integration testing.");
        
        String context = KeywordMatcher.extractRAGContext(doubt, Collections.singletonList(note));
        
        assertTrue("Context should contain the definition", context.contains("process of executing a program"));
        assertFalse("Context should NOT contain the TOC", context.contains("Table of Contents"));
    }

    @Test
    public void testTOCPenalty() {
        Doubt doubt = new Doubt("Testing types", "What are the types of testing?", "student", "123", "CS", "Eng", "FY");
        
        Note note = new Note();
        note.setTitle("Software Testing");
        note.setSubject("CS");
        note.setContent("Index\nTesting types .... page 5\nUnit testing .... page 6\n---SLIDE---\nThere are various types of testing: Unit, Integration, System.");
        
        String context = KeywordMatcher.extractRAGContext(doubt, Collections.singletonList(note));
        
        assertTrue("Context should pick the second slide", context.contains("Unit, Integration, System"));
        assertFalse("Context should avoid the Index slide", context.contains("page 5"));
    }
}
