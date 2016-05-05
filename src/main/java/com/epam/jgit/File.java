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

    public File(String startCommit) {
        this.startCommit = startCommit;
        this.listing = new LinkedList<>();
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
            if (0 == edit.getLengthA()) {
                if (edit.getBeginA() >= pos
                        && edit.getBeginA() < pos + block.getSize()) {
                    begin = number;
                    pos =+ block.getSize();
                    break;
                }
            } else {
                if (edit.getBeginA() + 1 >= pos
                        && edit.getBeginA() + 1 <= pos + block.getSize()) {
                    if (edit.getBeginA() + 1 > pos) {
                        blocks.add(new Block(edit.getBeginA() - pos, block.getId()));
                    }
                    begin = number;
                }
                if (0 <= begin) {
                    affectedCommits.add(block.getId());
                }
                if (edit.getEndA() >= pos
                        && edit.getEndA() <= pos + block.getSize()) {
                    blocks.add(new Block(edit.getLengthB(), commit));
                    if (edit.getEndA() < pos + block.getSize()) {
                        blocks.add(new Block(pos + block.getSize() - edit.getEndA(), block.getId()));
                    }
                    end = number;
                    pos =+ block.getSize();
                    break;
                }
            }
            pos =+ block.getSize();
            number++;
        }

        if (0 == edit.getLengthA()) {
            if (0 > begin) {
                int size = edit.getBeginA() - pos;
                if (0 != size) {
                    blocks.add(new Block(size, this.startCommit));
                }
                blocks.add(new Block(edit.getLengthB(), commit));
            } else {
                Block block = listing.get(begin);
                int sizeFirst = edit.getBeginA() - (pos - block.getSize());
                blocks.add(new Block(sizeFirst, block.getId()));
                blocks.add(new Block(edit.getLengthB(), commit));
                blocks.add(new Block(block.getSize() - sizeFirst, block.getId()));
            }
        } else {
            if (edit.getEndA() > pos) {
                blocks.add(new Block(edit.getLengthB(), commit));
                end = listing.size() - 1;
            }
        }
        final int beginIndex = begin;

        switch (edit.getType()) {
            case DELETE:
                IntStream.rangeClosed(beginIndex, end)
                        .forEach(i -> listing.remove(beginIndex));
                break;
            case REPLACE:
                IntStream.rangeClosed(beginIndex, end)
                        .forEach(i -> listing.remove(beginIndex));
                listing.addAll(beginIndex, blocks);
                break;
            case INSERT:
                if (0 <= begin) {
                    listing.remove(begin);
                }
                if (0 > begin) {
                    begin = listing.size();
                }
                listing.addAll(begin, blocks);
                break;
        }
        return affectedCommits.toArray(new String[]{});
    }

}
