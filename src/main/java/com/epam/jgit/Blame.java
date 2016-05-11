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
    private Map<String, Set<String>> blameLinks;

    public Blame(String id, HashMap<String, File> files) {
        this.initCommit = id;
        this.files = files;
    }

    public Blame(Blame[] blames, Commit[] commits, String id) {
        this.id = id;
        this.initCommit = blames[0].getInitCommit();
        this.files = mapDeepCopy(blames[0].getFiles());

        this.blameLinks = new HashMap<>();
        IntStream.range(0, blames.length)
                .forEach(num ->
                        commits[num].getChanges().entrySet().stream().forEach(entry -> {

                            int size = 0;
                            if (DiffEntry.ChangeType.ADD.equals(entry.getValue().getChangeTypeFile())) {
                                size = Arrays.stream(entry.getValue().getChanges())
                                        .mapToInt(Edit::getLengthB)
                                        .sum();
                            }

                            String filePath = DiffEntry.ChangeType.RENAME.equals(entry.getValue().getChangeTypeFile()) ?
                                    entry.getValue().getOldPath() : entry.getKey();
                            File findFile = files.get(filePath);
                            if (null == findFile) {
                                findFile = new File(this.id, size);
                                files.put(filePath, findFile);
                            }
                            File file = findFile;
                            switch (entry.getValue().getChangeTypeFile()) {
                                case RENAME:
                                    files.put(entry.getKey(), files.remove(entry.getValue().getOldPath()));
                                case MODIFY:
                                    Edit[] edits = entry.getValue().getChanges();
                                    IntStream.range(0, edits.length)
                                            .forEach(i -> {
                                                Arrays.stream(file.changeBlock(edits[edits.length - 1 - i], this.id))
                                                        .forEach(s -> {
                                                            Set<String> paths = this.blameLinks.get(s);
                                                            if (null == paths) {
                                                                paths = new HashSet<>();
                                                                this.blameLinks.put(s, paths);
                                                            }
                                                            paths.add(entry.getKey());
                                                        });
                                            });
                                    break;
                                case ADD:
                                    files.put(entry.getKey(), file);
                                    break;
                                case DELETE:
                                    Arrays.stream(file.getBlameCommits())
                                            .forEach(s -> {
                                                Set<String> paths = this.blameLinks.get(s);
                                                if (null == paths) {
                                                    paths = new HashSet<>();
                                                    this.blameLinks.put(s, paths);
                                                }
                                                paths.add(entry.getKey());
                                            });
                                    files.remove(entry.getKey());
                                    break;
                                case COPY:
                                    break;
                            }
                        }));
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
                .forEach(entry ->
                        newMap.put(entry.getKey(), new File(entry.getValue())));
        return newMap;
    }

    public Map<String, Set<String>> getBlameLinks() {
        return blameLinks;
    }
}
