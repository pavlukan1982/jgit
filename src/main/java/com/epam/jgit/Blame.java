package com.epam.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class Blame {
    private String id;
    private String initCommit;
    private Map<String, File> files;

    public Blame(String id, HashMap<String, File> files) {
        this.initCommit = id;
        this.files = files;
    }

    public Blame(Blame[] blames, Commit[] commits, String id) {
        this.id = id;
        this.initCommit = blames[0].getInitCommit();
        this.files = mapDeepCopy(blames[0].getFiles());

        Map<String, Set<String>> blameLinks = new HashMap<>();

        if (1 == blames.length) {
            commits[0].getChanges().entrySet().stream().forEach(entry -> {

                File findFile = this.files.get(entry.getKey());

                File file = (null == findFile)
                        ? new File(this.id, DiffEntry.ChangeType.ADD.equals(entry.getValue().getChangeTypeFile()) ?
                        Arrays.stream(entry.getValue().getChanges())
                                .mapToInt(Edit::getLengthB)
                                .sum() : 0)
                        : findFile;

                switch (entry.getValue().getChangeTypeFile()) {
                    case MODIFY:
                        if (null == findFile) {
                            files.put(entry.getKey(), file);
                        }
                        Edit[] edits = entry.getValue().getChanges();
                        IntStream.range(0, edits.length)
                                .forEach(i -> {
                                    file.changeBlock(edits[edits.length - 1 - i], this.id);
                                });
                        break;
                    case ADD:
                        files.put(entry.getKey(), file);
                        break;
                    case DELETE:
                        Set<String> files = blameLinks.get(this.id);
                        if (null == files) {
                            files = new HashSet<>();
                            blameLinks.put(this.id, files);
                        }
                        if (null == findFile) {
                            files.add(entry.getKey());
                        } else {
                            files.addAll(findFile.getBlameCommits());
                        }
                }
            });


        }
    }

    public String getId() {
        return id;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public String getInitCommit() {
        return initCommit;
    }

    private Map<String, File> mapDeepCopy(Map<String, File> map) {
        Map newMap = new HashMap<>(map.size());
        map.entrySet().stream()
                .forEach(entry -> newMap.put(entry.getKey(), new File(entry.getValue())));
        return newMap;
    }
}
