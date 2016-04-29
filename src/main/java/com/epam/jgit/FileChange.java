package com.epam.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;

import java.util.Map;

/**
 * Created by Andrei_Pauliukevich1 on 4/29/2016.
 */
public class FileChange {
    private DiffEntry.ChangeType changeType;
    private String oldPath;
    private String newPath;
    private Map<Edit.Type, Integer> changes;

    public FileChange(DiffEntry.ChangeType changeType, String oldPath, String newPath, Map<Edit.Type, Integer> changes) {
        this.changeType = changeType;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.changes = changes;
    }

    public DiffEntry.ChangeType getChangeType() {
        return changeType;
    }

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public Map<Edit.Type, Integer> getChanges() {
        return changes;
    }
}
