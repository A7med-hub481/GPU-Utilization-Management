package model;

/**
 * Interface for objects that are saved as one line in a text file.
 * Student, AccessRequest and Job implement it.
 */
public interface Saveable {

    // returns the object's data as one line, fields separated by "|"
    String toFileLine();
}
