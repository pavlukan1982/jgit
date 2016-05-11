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
    private String[] parents;

    public Blame(String id, HashMap<String, File> files) {
        this.initCommit = id;
        this.files = files;
        this.parents = null;
    }

    public Blame(String id, Blame[] blames, Commit[] commits, Map<String, Blame> blameMap) {
        this.id = id;
        this.initCommit = blames[0].getInitCommit();
        this.parents = Arrays.stream(commits)
                .map(commit -> commit.getId())
                .toArray(String[]::new);

        this.files = new HashMap<>();

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

                            File findFile = null;
                            if (!DiffEntry.ChangeType.ADD.equals((entry.getValue().getChangeTypeFile()))) {
                                findFile = findFile(this.id, blameMap);
                            }
                            if (null == findFile) {
                                findFile = new File(this.id, size);
                                files.put(filePath, findFile);
                            } else {
                                findFile = new File(findFile);
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

    public Map<String, Set<String>> getBlameLinks() {
        return blameLinks;
    }

    private File findFile(String path, Map<String, Blame> blameMap) {
        File file = this.files.get(path);
        List<String> childs = Arrays.asList(this.parents);
        Set<String> processed = new HashSet<>();
        while (0 < childs.size() && null == file) {
            List<String> parents = new ArrayList<>();
            for (String child : childs) {
                Blame blame = blameMap.get(child);
                file = blame.getFiles().get(path);
                if (null == file) {
                    if (null != blame.getParents() && processed.add(child)) {
                        parents.addAll(Arrays.asList(blame.getParents()));
                    }
                } else {
                    break;
                }
            }
            childs = parents;
        }
        return  file;
    }

    public String[] getParents() {
        return parents;
    }
}
