package com.epam.jgit;

import org.eclipse.jgit.diff.Edit;

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

    public String[] changeBlock(Edit edit) {
        if (0 == this.listing.size()) {
            switch (edit.getType()) {
                case DELETE:
                    break;
                case INSERT:
                case REPLACE:
                    this.listing.add(new Block(edit.getLengthB(), this.startCommit));
                    break;
            }
            return Edit.Type.INSERT.equals(edit.getType())
                    ? new String[0] : new String[]{this.startCommit};
        } else {
            int len = 0, pos = 0;
            int begin = -1, end = 0;
            for (Block block : this.listing) {
                len =+ block.getSize();
                if (edit.getBeginA() + 1 >= len && 0 > begin) {
                    begin = pos;
                }
                if (edit.getEndA() <= len) {
                    end = pos;
                    break;
                }

            }

        }

        return null;
    }
}
