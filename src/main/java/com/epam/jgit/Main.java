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

    public static String sha = "79f127f678702483174368cdb61c58485bec6a0a";
    public static int depth = 100;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, String[]> commits = new HashMap<>();
        Map<String, Commit[]> changes = new HashMap<>();
        Map<String, String[]> blameLinks = new HashMap<>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File("d:/jira/projects/140-android/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            RevWalk walk = new RevWalk(repository);

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
                    String[] parents = commits.put(id.toObjectId().getName(), Arrays.stream(childCommit.getParents())
                            .map(RevCommit::toObjectId)
                            .map(ObjectId::getName)
                            .toArray(String[]::new));

                    if (null == parents) {
                        parentList.addAll(Arrays.asList(childCommit.getParents()));

                        Commit[] commitArray = new Commit[childCommit.getParents().length];
                        for (int j = 0; j < childCommit.getParents().length; j++) {

                            RevCommit parentCommit = walk.parseCommit(childCommit.getParent(j));

                            RevTree childTree = walk.parseTree(childCommit.getTree());
                            RevTree parentTree = walk.parseTree(parentCommit.getTree());

                            List<DiffEntry> entries = diffFormatter.scan(parentTree, childTree);

                            Map<String, FileChange> fileChanges = new HashMap<>();
                            for( DiffEntry entry : entries ) {
                                FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                                List<? extends HunkHeader> hunks = fileHeader.getHunks();

                                String path = DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())
                                        ? entry.getOldPath() : entry.getNewPath();

                                fileChanges.put(path, new FileChange(entry.getChangeType(), hunks.stream()
                                        .map(HunkHeader::toEditList)
                                        .flatMap(Collection::stream)
                                        .toArray(Edit[]::new)));
                            }

                            commitArray[j] = new Commit(childCommit.getParent(j).toObjectId().getName(), fileChanges);

                        }

                        changes.put(id.toObjectId().getName(), commitArray);
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
        System.out.println("Total commits : " + commits.size());


    }
}
