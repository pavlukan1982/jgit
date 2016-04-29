package com.epam.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by Andrei_Pauliukevich1 on 4/26/2016.
 */
public class Main {

    public static String sha = "34c0975f9c9d725079e3c222360b240b1889ae4f";
    public static int depth = 2;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, String[]> links = new HashMap<>();
        Map<String, List<FileChange>> changes = new HashMap<>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File("d:/jira/projects/140-android/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
             RevWalk walk = new RevWalk(repository)) {

            ObjectId commit = repository.resolve(sha);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setPathFilter(new ExtensionTreeFilter("java"));
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(0);

            Set<ObjectId> childList = new HashSet<>();
            childList.add(commit);
            Set<ObjectId> parentList = new HashSet<>();

            for (int i = depth; i > 0 ; i--) {
                for (ObjectId id : childList) {
                    RevCommit childCommit = walk.parseCommit(id);
                    String[] parents = links.put(id.toObjectId().getName(), Arrays.stream(childCommit.getParents())
                            .map(RevCommit::toObjectId)
                            .map(ObjectId::getName)
                            .toArray(String[]::new));
                    if (null == parents) {
                        parentList.addAll(Arrays.asList(childCommit.getParents()));
                        for (int j = 0; j < childCommit.getParents().length; j++) {

                            RevCommit parentCommit = walk.parseCommit(childCommit.getParent(j));
                            RevTree childTree = walk.parseTree(childCommit.getTree());
                            RevTree parentTree = walk.parseTree(parentCommit.getTree());

                            List<DiffEntry> entries = diffFormatter.scan(parentTree, childTree);

                            List<FileChange> fileChanges = new ArrayList<>();
                            for( DiffEntry entry : entries ) {
                                FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                                List<? extends HunkHeader> hunks = fileHeader.getHunks();

                                Map<Edit.Type, Integer> changeMap = new EnumMap(Edit.Type.class);

                                hunks.stream()
                                        .map(hunkHeader -> hunkHeader.toEditList())
                                        .flatMap(edits -> edits.stream())
                                        .forEach(edit -> {

                                            int lineCount = Edit.Type.DELETE.equals(edit.getType()) ? edit.getLengthA() : edit.getLengthB();
                                            Integer number = changeMap.put(edit.getType(), lineCount);
                                            if (null != number) {
                                                changeMap.put(edit.getType(), number + lineCount);
                                            }});

                                fileChanges.add(new FileChange(entry.getChangeType(),
                                        entry.getOldPath(),
                                        entry.getNewPath(),
                                        changeMap));
                            }
                            changes.put(id.toObjectId().getName(), fileChanges);
                        }
                    }
                }

                childList = new HashSet<>(parentList);
                parentList.clear();

                if (0 == childList.size()) {
                    System.out.println("Depth : " + (depth - i));
                    break;
                }
            }

        }

        System.out.println("Total time : " + (ZonedDateTime.now().toInstant().toEpochMilli() - start));
        System.out.println("Total links : " + links.size());
    }
}
