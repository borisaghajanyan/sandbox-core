package com.baghajanyan.sandbox.core.datastore;

import java.util.List;
import java.util.Map;

public interface DataStore {
    void insert(String table, Map<String, Object> row);

    List<Map<String, Object>> select(String table);

    void clear();
}
