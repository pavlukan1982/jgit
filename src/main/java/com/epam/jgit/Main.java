package com.epam.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by Andrei_Pauliukevich1 on 4/26/2016.
 */
public class Main {

    public static String sha = "e329c40fd7ece58cf09ea3cf02b95f9fc5d8e79b";
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

                Set<ObjectId> parentList = new HashSet<>();
                parentList.add(commit);
                Set<ObjectId> childList = new HashSet<>();

                for (int i = depth; i > 0 ; i--) {
                    for (ObjectId id : parentList) {
                        RevCommit revCommit = walk.parseCommit(id);
                        String[] childs = links.put(id.toObjectId().getName(), Arrays.stream(revCommit.getParents())
                                .map(RevCommit::toObjectId)
                                .map(ObjectId::getName)
                                .toArray(String[]::new));
                        if (null == childs) {
                            childList.addAll(Arrays.asList(revCommit.getParents()));
                        }
                    }

                    parentList = new HashSet<>(childList);
                    childList.clear();

                    if (0 == parentList.size()) {
                        System.out.println("Depth : " + (depth - i));
                        break;
                    }
                }




            }
        }

        System.out.println("Total time : " + (ZonedDateTime.now().toInstant().toEpochMilli() - start));
        System.out.println("Total links : " + links.size());
    }
}
