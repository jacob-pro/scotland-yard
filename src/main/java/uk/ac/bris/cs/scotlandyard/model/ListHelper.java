package uk.ac.bris.cs.scotlandyard.model;

import java.util.List;

public class ListHelper {

    public static boolean containsDuplicates(List<?> list) {
        return list.size() != list.stream().distinct().count();
    }

}
