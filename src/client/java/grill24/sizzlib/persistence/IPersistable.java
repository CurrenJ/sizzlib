package grill24.sizzlib.persistence;

import java.io.File;

public interface IPersistable {
    void fromJson(String json) throws IllegalAccessException;

    String toJson() throws IllegalAccessException;

    File getFile();
}