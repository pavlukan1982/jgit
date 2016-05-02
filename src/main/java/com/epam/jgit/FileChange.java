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
    private Map<Edit.Type, Integer> changes;

    public FileChange(DiffEntry.ChangeType changeType, String oldPath, Map<Edit.Type, Integer> changes) {
        this.changeType = changeType;
        this.oldPath = oldPath;
        this.changes = changes;
    }

    public DiffEntry.ChangeType getChangeType() {
        return changeType;
    }

    public String getOldPath() {
        return oldPath;
    }

    public Map<Edit.Type, Integer> getChanges() {
        return changes;
    }
}
