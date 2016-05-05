package com.epam.jgit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class Blame {
    private String id;
    private String initCommit;
    private Map<String, File> files;

    public Blame(String id) {
        this.initCommit = id;
        this.files = new HashMap<>();
    }

    public Blame(Blame[] blames, Commit[] commits, String id) {
        this.id = id;
        this.initCommit = blames[0].getInitCommit();
        if (1 == blames.length) {
            this.files = mapDeepCopy(blames[0].getFiles());
            commits[0].getChanges().entrySet().stream().forEach(entry -> {
                File findFile = this.files.get(entry.getKey());
                File file = (null == findFile)
                        ? new File(this.initCommit) : findFile;
                if (null == findFile) {
                    files.put(entry.getKey(), file);
                }
                Arrays.stream(entry.getValue().getChanges())
                        .forEach(edit -> {
                            file.changeBlock(edit, this.id);
                        });
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
