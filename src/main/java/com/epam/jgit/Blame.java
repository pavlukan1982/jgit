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

                            // TODO: 5/6/2016 test for speed and replase with simple methods
                            File file = files.getOrDefault(entry.getKey(), new File(this.id, size));
                            files.putIfAbsent(entry.getKey(), file);

                            switch (entry.getValue().getChangeTypeFile()) {
                                case RENAME:
                                    files.put(entry.getKey(), files.remove(entry.getValue().getOldPath()));
                                case MODIFY:
                                    Edit[] edits = entry.getValue().getChanges();
                                    IntStream.range(0, edits.length)
                                            .forEach(i -> {
                                                Arrays.stream(file.changeBlock(edits[edits.length - 1 - i], this.id))
                                                        .forEach(s -> {
                                                            Set<String> set = this.blameLinks.getOrDefault(s, new HashSet<>());
                                                            set.add(entry.getKey());
                                                            this.blameLinks.putIfAbsent(s, new HashSet<>());
                                                        });
                                            });
                                    break;
                                case ADD:
                                    files.put(entry.getKey(), file);
                                    break;
                                case DELETE:
                                    Arrays.stream(file.getBlameCommits())
                                            .forEach(s -> {
                                                Set<String> set = this.blameLinks.getOrDefault(s, new HashSet<>());
                                                set.add(entry.getKey());
                                                this.blameLinks.putIfAbsent(s, new HashSet<>());
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
                .forEach(entry -> newMap.put(entry.getKey(), new File(entry.getValue())));
        return newMap;
    }

    public Map<String, Set<String>> getBlameLinks() {
        return blameLinks;
    }
}
