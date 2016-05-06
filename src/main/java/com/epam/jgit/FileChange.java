package com.epam.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;

/**
 * Created by Andrei_Pauliukevich1 on 4/29/2016.
 */
public class FileChange {
    private DiffEntry.ChangeType changeTypeFile;
    private Edit[] changes;
    private String oldPath;

    public FileChange(DiffEntry.ChangeType changeTypeFile, Edit[] changes, String oldPath) {
        this.changeTypeFile = changeTypeFile;
        this.changes = changes;
        this.oldPath = oldPath;
    }

    public DiffEntry.ChangeType getChangeTypeFile() {
        return changeTypeFile;
    }

    public Edit[] getChanges() {
        return changes;
    }

    public String getOldPath() {
        return oldPath;
    }
}
