package com.epam.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;

/**
 * Created by Andrei_Pauliukevich1 on 4/29/2016.
 */
public class FileChange {
    private DiffEntry.ChangeType changeTypeFile;
    private Edit[] changes;

    public FileChange(DiffEntry.ChangeType changeTypeFile, Edit[] changes) {
        this.changeTypeFile = changeTypeFile;
        this.changes = changes;
    }

    public DiffEntry.ChangeType getChangeTypeFile() {
        return changeTypeFile;
    }

    public Edit[] getChanges() {
        return changes;
    }
}
