package com.epam.jgit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class Blame {
    private String id;
    private Map<String, File> files;

    public Blame(String id) {
        this.id = id;
        this.files = new HashMap<>();
    }

    public Blame(Blame[] blames, Commit[] commits) {
        if (1 == blames.length) {
            this.files = new HashMap<>(blames[0].getFiles());
            commits[0].getChanges().entrySet().stream().forEach(entry -> {
                File findFile = this.files.get(entry.getKey());
                final File file = null == findFile
                        ? new File(this.id) : findFile;
                Arrays.stream(entry.getValue().getChanges())
                        .forEach(edit -> {
                            file.changeBlock(edit);
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
}
