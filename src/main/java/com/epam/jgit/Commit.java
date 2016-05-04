package com.epam.jgit;

import java.util.Map;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class Commit {
    private String id;
    private Map<String, FileChange> changes;

    public Commit(String id, Map<String, FileChange> changes) {
        this.id = id;
        this.changes = changes;
    }

    public String getId() {
        return id;
    }

    public Map<String, FileChange> getChanges() {
        return changes;
    }
}
