package grill24.sizzlib.persistence;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;

import java.io.File;

public interface IPersistable {
    void fromJson(String json) throws IllegalAccessException;

    JsonObject toJson() throws IllegalAccessException;

    File getFile();
}