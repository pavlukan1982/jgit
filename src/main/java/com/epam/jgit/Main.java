package com.epam.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by Andrei_Pauliukevich1 on 4/26/2016.
 */
public class Main {

    public static String sha = "79f127f678702483174368cdb61c58485bec6a0a";
    public static int depth = 4000;

    public static void main(String[] args) throws IOException, GitAPIException{

        long start = ZonedDateTime.now().toInstant().toEpochMilli();

        Map<String, String[]> links = new HashMap<>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File("d:/jira/projects/140-android/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {

            ObjectId commit = repository.resolve(sha);

            try (RevWalk walk = new RevWalk(repository)) {

                Set<ObjectId> childList = new HashSet<>();
                childList.add(commit);
                Set<ObjectId> parentList = new HashSet<>();

                for (int i = depth; i > 0 ; i--) {
                    for (ObjectId id : childList) {
                        RevCommit revCommit = walk.parseCommit(id);
                        String[] childs = links.put(id.toObjectId().getName(), Arrays.stream(revCommit.getParents())
                                .map(RevCommit::toObjectId)
                                .map(ObjectId::getName)
                                .toArray(String[]::new));
                        if (null == childs) {
                            parentList.addAll(Arrays.asList(revCommit.getParents()));
                        }
                    }

                    childList = new HashSet<>(parentList);
                    parentList.clear();

                    if (0 == childList.size()) {
                        System.out.println("Depth : " + (depth - i));
                        break;
                    }
                }

                try (TreeWalk treeWalk = new TreeWalk(repository);
                     Git git = new Git(repository))
                {
                    treeWalk.addTree(walk.parseCommit(commit).getTree());
                    treeWalk.setRecursive(true);
                    int i = 0;

                    while (treeWalk.next()) {
                        if (FileMode.REGULAR_FILE.equals(treeWalk.getFileMode()) ||
                                FileMode.EXECUTABLE_FILE.equals(treeWalk.getFileMode())) {
                            System.out.println(treeWalk.getFileMode() + " " + treeWalk.getPathString());
                            i++;
                            Iterable<RevCommit> commits = git.log().addPath(treeWalk.getPathString()).call();
                            for (RevCommit rcommit : commits) {
                                System.out.println(rcommit.toObjectId().getName());
                            }



                        }
                    }
                    System.out.println("Total number of files : " + i);
                }


            }
        }

        System.out.println("Total time : " + (ZonedDateTime.now().toInstant().toEpochMilli() - start));
        System.out.println("Total links : " + links.size());
    }
}
