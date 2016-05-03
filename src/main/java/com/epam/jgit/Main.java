package com.epam.jgit;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
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
import java.util.stream.IntStream;

/**
 * Created by Andrei_Pauliukevich1 on 4/26/2016.
 */
public class Main {

    public static String sha = "7926e62482759d3c5553916827b3e85dc356dfe3";
    public static int depth = 5;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, String[]> commits = new HashMap<>();
        Map<String, Map<String, FileChange>> changes = new HashMap<>();
        Map<String, String[]> blameLinks = new HashMap<>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File("d:/jira/projects/140-android/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            RevWalk walk = new RevWalk(repository);

            ObjectId commit = repository.resolve(sha);
            BlameCommand blame = new BlameCommand(repository);

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
                        for (int j = 0; j < childCommit.getParents().length; j++) {

                            RevCommit parentCommit = walk.parseCommit(childCommit.getParent(j));
                            blame.setStartCommit(childCommit.getParent(j));
                            System.out.println(childCommit.getParent(j));


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

                            changes.put(id.toObjectId().getName(), fileChanges);
                            String[] strings = fileChanges.entrySet()
                                    .stream()
                                    .filter(entry -> !DiffEntry.ChangeType.ADD.equals(entry.getValue().getChangeTypeFile()))
                                    .flatMap(entry -> {
                                        BlameResult blameResult;
                                        try {
                                            blameResult = blame.setFilePath(entry.getKey()).call();
                                        } catch (GitAPIException e) {
                                            throw new RuntimeException(e);
                                        }
                                        return Arrays.stream(entry.getValue().getChanges())
                                                .filter(edit -> !edit.isEmpty())
                                                .flatMap(edit -> IntStream.range(edit.getBeginA(), edit.getEndA())
                                                        .mapToObj(blameResult::getSourceCommit));
                                    })
                                    .map(revCommit -> revCommit.toObjectId().getName())
                                    .distinct()
                                    .toArray(String[]::new);
                            if (0 < strings.length) {
                                blameLinks.put(id.toObjectId().getName(), strings);
                            }

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
        System.out.println("Total commits : " + commits.size());


    }
}
