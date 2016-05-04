package com.epam.jgit;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class Block {
    private int size;
    private String id;

    public Block(int size, String id) {
        this.size = size;
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public String getId() {
        return id;
    }
}
