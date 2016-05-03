package com.epam.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;

import java.util.Map;

/**
 * Created by Andrei_Pauliukevich1 on 4/29/2016.
 */
public class FileChange {
    private DiffEntry.ChangeType changeTypeFile;
    private String oldPath;
    private Map<Edit.Type, Integer> changes;
    private Edit.Type changeType;

    public FileChange(DiffEntry.ChangeType changeTypeFile, String oldPath, Map<Edit.Type, Integer> changes) {
        this.changeTypeFile = changeTypeFile;
        this.oldPath = oldPath;
        this.changes = changes;
        if (1 < this.changes.size()) {
            this.changeType = Edit.Type.REPLACE;
        } else if ( 1 == this.changes.size()) {
            this.changeType = changes.entrySet().iterator().next().getKey();
        }
    }

    public DiffEntry.ChangeType getChangeTypeFile() {
        return changeTypeFile;
    }

    public String getOldPath() {
        return oldPath;
    }

    public Map<Edit.Type, Integer> getChanges() {
        return changes;
    }

    public Edit.Type getChangeType() {
        return changeType;
    }
}
