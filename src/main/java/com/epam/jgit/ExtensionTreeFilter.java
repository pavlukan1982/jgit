package com.epam.jgit;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Andrei_Pauliukevich1 on 4/29/2016.
 */
public class ExtensionTreeFilter extends TreeFilter {

    private String[] extensions;

    public ExtensionTreeFilter(String[] extensions) {
        this.extensions = extensions;
    }

    public ExtensionTreeFilter(String extensions) {
        this(Arrays.stream(extensions.split(","))
                .map(s -> s.trim().toLowerCase())
                .toArray(String[]::new));
    }

    @Override
    public boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
        return null != Arrays.stream(extensions)
                .filter(s -> FileMode.TREE.equals(walker.getFileMode()) ||
                        walker.getPathString().toLowerCase().endsWith(s))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean shouldBeRecursive() {
        return false;
    }

    @Override
    public TreeFilter clone() {
        return this;
    }
}
