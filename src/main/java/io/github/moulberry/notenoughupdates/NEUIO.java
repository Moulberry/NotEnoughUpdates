package io.github.moulberry.notenoughupdates;

import org.kohsuke.github.*;

import java.io.IOException;
import java.util.*;

public class NEUIO {

    private final String accessToken;

    /**
     * THIS CLASS PROVIDES METHODS FOR INTERFACING WITH THE GIT REPOSITORY NotEnoughUpdates-REPO. THIS REPOSITORY
     * CONTAINS ALL THE JSON ITEMS. THIS SHOULD NOT BE A PERMANENT SOLUTION AND I SHOULD LOOK AT USING SOME FORM OF
     * HOSTING SERVICE OTHER THAN A GIT REPOSITORY IF THE USERBASE OF THE MOD GROWS SIGNIFICANTLY. UNFORTUNATELY I
     * CANT AFFORD HOSTING RIGHT NOW SO THIS IS WHAT YOU GET AND GITHUB WILL PROBABLY THROW A FIT IF A LARGE NUMBER
     * OF USERS START DOWNLOADING FROM THE REPO ALL AT ONCE.
     */

    public NEUIO(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Creates a new branch, commits to it with a single file change and submits a pull request from the new branch
     * back to the master branch.
     */
    public boolean createNewRequest(String newBranchName, String prTitle, String prBody, String filename, String content) {
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(accessToken).build();
            System.out.println("Getting repo");

            //https://github.com/Moulberry/NotEnoughUpdates-REPO
            GHRepository repo = github.getRepositoryById("247692460");

            System.out.println("Getting last commit");
            String lastCommitSha = repo.getRef("heads/master").getObject().getSha();
            System.out.println("Last master commit sha: " + lastCommitSha);

            String lastTreeSha = repo.getCommit(lastCommitSha).getTree().getSha();

            GHTreeBuilder tb = repo.createTree();
            tb.baseTree(lastTreeSha);
            tb.add(filename, content, false);
            GHTree tree = tb.create();
            System.out.println("Created new tree: " + tree.getSha());

            GHCommitBuilder cb = repo.createCommit();
            cb.message(prTitle);
            cb.tree(tree.getSha());
            cb.parent(lastCommitSha);
            GHCommit commit = cb.create();
            System.out.println("Created commit: " + commit.getSHA1());

            repo.createRef("refs/heads/"+newBranchName, commit.getSHA1());
            System.out.println("Set new branch head to commit.");

            repo.createPullRequest(prTitle, newBranchName, "master", prBody);
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param oldShas Map from filename (eg. BOW.json) to the sha in the local repository
     * @return Map from filename to the new shas
     */
    public Map<String, String> getChangedItems(Map<String, String> oldShas) {
        HashMap<String, String> changedFiles = new HashMap<>();
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(accessToken).build();
            GHRepository repo = github.getRepositoryById("247692460");

            for(GHTreeEntry treeEntry : repo.getTreeRecursive("master", 1).getTree()) {
                if(treeEntry.getPath().startsWith("items/")) {
                    String[] split = treeEntry.getPath().split("/");
                    String name = split[split.length-1];

                    String oldSha = oldShas.get(name);
                    if(!treeEntry.getSha().equals(oldSha)) {
                        changedFiles.put(name, treeEntry.getSha());
                    }
                }
            }

            /*for(GHContent content : repo.getDirectoryContent("items")) {
                String oldSha = oldShas.get(content.getName());
                if(!content.getSha().equals(oldSha)) {
                    changedFiles.put(content.getName(), content.getSha());
                }
            }*/
        } catch(IOException e) {
            return null;
        }
        return changedFiles;
    }

    public Set<String> getRemovedItems(Set<String> currentlyInstalled) {
        Set<String> removedItems = new HashSet<>();
        Set<String> repoItems = new HashSet<>();
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(accessToken).build();
            GHRepository repo = github.getRepositoryById("247692460");

            for(GHTreeEntry treeEntry : repo.getTreeRecursive("master", 1).getTree()) {
                String[] split = treeEntry.getPath().split("/");
                repoItems.add(split[split.length-1].split("\\.")[0]);
            }
        } catch(IOException e) {
            e.printStackTrace();
            return removedItems;
        }
        removedItems.addAll(currentlyInstalled);
        removedItems.removeAll(repoItems);
        return removedItems;
    }

    /**
     * Takes set of filename (eg. BOW.json) and returns map from that filename to the individual download link.
     */
    public Map<String, String> getItemsDownload(Set<String> filename) {
        HashMap<String, String> downloadUrls = new HashMap<>();
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(accessToken).build();
            GHRepository repo = github.getRepositoryById("247692460");

            for(GHContent content : repo.getDirectoryContent("items")) {
                if(filename.contains(content.getName())) {
                    downloadUrls.put(content.getName(), content.getDownloadUrl());
                }
            }
        } catch(IOException e) { }
        return downloadUrls;
    }
}
