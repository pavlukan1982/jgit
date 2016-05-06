package com.epam.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by Andrei_Pauliukevich1 on 4/26/2016.
 */
public class Main {

    public static String sha = "79f127f678702483174368cdb61c58485bec6a0a";
    public static int depth = 50;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, Commit[]> changes = new HashMap<>();
        Map<String, String[]> blameLinks = new HashMap<>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File("d:/jira/projects/140-android/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            RevWalk walk = new RevWalk(repository);

            ObjectId startCommit = repository.resolve(sha);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            ExtensionTreeFilter filter = new ExtensionTreeFilter("java");
            diffFormatter.setPathFilter(filter);
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(0);

            RenameDetector renameDetector = new RenameDetector(repository);

            Set<ObjectId> childList = new HashSet<>();
            childList.add(startCommit);
            Set<ObjectId> parentList = new HashSet<>();

            for (int i = depth; i > 0 ; i--) {
                for (ObjectId id : childList) {
                    RevCommit childCommit = walk.parseCommit(id);

                    Commit[] commits = changes.get(id.toObjectId().getName());

                    if (null == commits) {
                        RevCommit[] parents = childCommit.getParents();
                        parentList.addAll(Arrays.asList(parents));

                        Commit[] commitArray = new Commit[parents.length];
                        for (int j = 0; j < parents.length; j++) {
                            renameDetector.reset();

                            RevCommit parentCommit = walk.parseCommit(childCommit.getParent(j));

                            renameDetector.addAll(diffFormatter.scan(parentCommit.getTree(), childCommit.getTree()));
                            List<DiffEntry> entries = renameDetector.compute();

                            Map<String, FileChange> fileChanges = new HashMap<>();
                            for( DiffEntry entry : entries ) {
                                FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                                List<? extends HunkHeader> hunks = fileHeader.getHunks();

                                String path = DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())
                                        ? entry.getOldPath() : entry.getNewPath();

                                fileChanges.put(path, new FileChange(
                                        entry.getChangeType(),
                                        hunks.stream()
                                                .map(HunkHeader::toEditList)
                                                .flatMap(Collection::stream)
                                                .toArray(Edit[]::new),
                                        entry.getOldPath())
                                );
                            }

                            commitArray[j] = new Commit(childCommit.getParent(j).toObjectId().getName(),
                                    depth - i + 1,
                                    fileChanges);

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

            Set<String> processed = new HashSet<>();
            HashMap<String, Blame> blameMap = new HashMap<>();


            String beginCommit = changes.entrySet().stream()
                    .filter(entry -> 0 == entry.getValue().length)
                    .map(entry -> entry.getKey())
                    .findAny()
                    .orElse(null);
            if (null == beginCommit) {
                RevCommit commit = walk.parseCommit(startCommit);
                while (0 != commit.getParentCount()) {
                    commit = walk.parseCommit(commit.getParent(0));
                }
                beginCommit = commit.toObjectId().getName();
            }
            String initCommit = beginCommit;

            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.setFilter(filter.clone());
            treeWalk.setRecursive(true);

            changes.entrySet().stream()
                    .forEach(entry -> Arrays.stream(entry.getValue())
                            .filter(commit -> {
                                Commit[] commits = changes.get(commit.getId());
                                if (null != commits && 0 == commits.length) {
                                    processed.add(commit.getId());
                                }
                                return (null == commits) || (0 == commits.length);})
                            .forEach(commit -> {

                                HashMap<String, com.epam.jgit.File> files = new HashMap<>();

                                try {
                                    RevCommit revCommit = walk.parseCommit(repository.resolve(commit.getId()));
                                    treeWalk.reset(revCommit.getTree());

                                    while (treeWalk.next()) {
                                        ObjectStream stream = repository.open(treeWalk.getObjectId(0)).openStream();
                                        LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream));
                                        while ((reader.readLine()) != null);
                                        files.put(treeWalk.getPathString(), new com.epam.jgit.File(initCommit, reader.getLineNumber()));
                                    }

                                } catch (IOException e) {
                                    new RuntimeException(e);
                                }
                                blameMap.put(commit.getId(), new Blame(commit.getId(), files));
                            }));


            while (processed.size() != changes.size()) {

                changes.entrySet().stream()
                        .filter(entry -> !processed.contains(entry.getKey()))
                        .forEach(entry -> {
                            Blame[] blames = Arrays.stream(entry.getValue())
                                    .map(commit -> blameMap.get(commit.getId()))
                                    .filter(blame -> null != blame)
                                    .toArray(Blame[]::new);
                            if (entry.getValue().length == blames.length) {
                                blameMap.put(entry.getKey(), new Blame(blames, entry.getValue(), entry.getKey()));
                                processed.add(entry.getKey());
                            }
                        });
                ;

            }

            System.out.println("Total time : " + (ZonedDateTime.now().toInstant().toEpochMilli() - start));
            System.out.println("Total commits : " + changes.size());

        }



    }
}
