package com.epam.jgit;

import org.eclipse.jgit.diff.Edit;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Andrei_Pauliukevich1 on 5/4/2016.
 */
public class File {
    private String startCommit;
    private List<Block> listing;

    public File(String commit, int size) {
        this.startCommit = commit;
        this.listing = new LinkedList<>();
        if (0 < size) {
            this.listing.add(new Block(size, commit));
        }
    }

    public File(File file) {
        this.startCommit = file.getStartCommit();
        this.listing = file.getListing().stream()
                .map(block -> new Block(block))
                .collect(Collectors.toList());
    }

    public String getStartCommit() {
        return startCommit;
    }

    public List<Block> getListing() {
        return listing;
    }

    public String[] changeBlock(Edit edit, String commit) {
        if (0 == this.listing.size()) {
            this.listing.add(new Block(edit.getEndA(), startCommit));
        }
        List<Block> blocks = new ArrayList<>();
        Set<String> affectedCommits = new HashSet<>();
        int pos = 0, number = 0, begin = -1, end = -1;
        for (Block block : this.listing) {
            pos += block.getSize();
            if (edit.getBeginA() + 1 <= pos) {
                int size = edit.getBeginA() - (pos - block.getSize());
                if (0 < size) {
                    blocks.add(new Block(edit.getBeginA() - (pos - block.getSize()), block.getId()));
                }
                begin = number;
            }
            if ((0 <= begin) &&
                    (0 < edit.getLengthA())) {
                affectedCommits.add(block.getId());
            }
            if (edit.getEndA() <= pos) {
                blocks.add(new Block(edit.getLengthB(), commit));
                int size = pos - edit.getEndA();
                if (0 < size) {
                    blocks.add(new Block(pos - edit.getEndA(), block.getId()));
                }
                end = number;
                break;
            }
            number++;
        }

        if (edit.getEndA() > pos) {
            if (0 == edit.getLengthA()) {
                blocks.add(new Block(edit.getBeginA() + 1 - pos, startCommit));
            }
            blocks.add(new Block(edit.getLengthB(), commit));
            end = listing.size() - 1;
        }
        final int beginIndex = (0 > begin) ? listing.size() : begin;

        IntStream.rangeClosed(beginIndex, end)
                .forEach(i -> listing.remove(beginIndex));

        switch (edit.getType()) {
            case DELETE:
                break;
            case REPLACE:
            case INSERT:
                listing.addAll(beginIndex, blocks);
                break;
        }
        return affectedCommits.toArray(new String[]{});
    }

    public String[] getBlameCommits() {
        return listing.stream()
                .map(Block::getId)
                .distinct()
                .toArray(String[]::new);
    }

}
