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

    public static String sha = "657edcc157b52cfa3f37775839df05aeb33c1f71";
    public static int depth = 20;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, String[]> commits = new HashMap<>();
        Map<String, Map<String, FileChange>> changes = new HashMap<>();

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
                    String[] parents = commits.put(id.toObjectId().getName(), Arrays.stream(childCommit.getParents())
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

                            Map<String, FileChange> fileChanges = new HashMap<>();
                            for( DiffEntry entry : entries ) {
                                FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                                List<? extends HunkHeader> hunks = fileHeader.getHunks();

                                String path = DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())
                                        ? entry.getOldPath() : entry.getNewPath();

                                FileChange fileChange = fileChanges.get(path);

                                Map<Edit.Type, Integer> changeMap = null == fileChange
                                        ? new EnumMap(Edit.Type.class) : fileChange.getChanges();

                                hunks.stream()
                                        .map(HunkHeader::toEditList)
                                        .flatMap(Collection::stream)
                                        .forEach(edit -> {
                                            int lineCount = Edit.Type.DELETE.equals(edit.getType()) ? edit.getLengthA() : edit.getLengthB();
                                            Integer number = changeMap.put(edit.getType(), lineCount);
                                            if (null != number) {
                                                changeMap.put(edit.getType(), number + lineCount);
                                            }});

                                fileChanges.put(path, new FileChange(entry.getChangeType(),
                                        entry.getOldPath(),
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

        System.out.println("Total commits : " + commits.size());

        // create blame graph

        Map<String, Map<String, Edit.Type>> blameLinks = new HashMap<>();
        List<String> parents = new ArrayList<>();
        List<String> childs = new ArrayList<>();
        Set<String> visitedNodes = new HashSet<>();

        for (Map.Entry<String, String[]> entry: commits.entrySet()) {

            Map<String, FileChange> fileChangeMap = changes.get(entry.getKey());
            if (null == fileChangeMap ) {
                continue;
            }


            HashMap<String, Edit.Type> blame = new HashMap<>();

            for (Map.Entry<String, FileChange> fileChangeEntry : fileChangeMap.entrySet()) {

                if (DiffEntry.ChangeType.ADD.equals(fileChangeEntry.getValue().getChangeTypeFile())) {
                    continue;
                }

                visitedNodes.clear();
                childs.clear();
                childs.addAll(Arrays.asList(commits.get(entry.getKey())));

                while (0 < childs.size()) {
                    childs.stream()
                            .forEach(child -> {
                                if (visitedNodes.add(child)) {

                                    Map<String, FileChange> changedFiles = changes.get(child);
                                    FileChange fileChange = null == changedFiles
                                            ? null : changedFiles.get(fileChangeEntry.getKey());
                                    String[] commitChilds = commits.get(child);
                                    if (null != commitChilds) {
                                        if (null != fileChange || 0 == commitChilds.length) {
                                            Edit.Type changeType = blame.get(child);
                                            if (null == changeType) {
                                                blame.put(child, fileChangeEntry.getValue().getChangeType());
                                            } else {
                                                if (!Edit.Type.REPLACE.equals(changeType)
                                                        && !changeType.equals(fileChangeEntry.getValue().getChangeType())) {
                                                    blame.put(child, Edit.Type.REPLACE);
                                                }
                                            }
                                        } else {
                                            parents.addAll(Arrays.asList(commits.get(child)));
                                        }
                                    }
                                }});
                    childs = new ArrayList<>(parents);
                    parents.clear();

                }

            }
            if (0 < blame.size()) {
                blameLinks.put(entry.getKey(), blame);
            }
        }


        System.out.println("Total time : " + (ZonedDateTime.now().toInstant().toEpochMilli() - start));
        System.out.println("Found blame commits : " + blameLinks.size());


    }
}
