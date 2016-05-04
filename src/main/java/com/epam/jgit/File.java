package com.epam.jgit;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class File {
    private String startCommit;
    private List<Block> listing;

    public File(String startCommit) {
        this.startCommit = startCommit;
        this.listing = new LinkedList<>();
    }

    public String getStartCommit() {
        return startCommit;
    }

    public List<Block> getListing() {
        return listing;
    }
}
