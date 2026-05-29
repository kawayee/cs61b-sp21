package gitlet;

import java.io.File;
import java.util.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  The structure of a .gitlet directory is as follows:
 *
 *  .gitlet/
 *  ├── commits/       -- serialized Commit objects keyed by SHA-1
 *  ├── blobs/         -- raw file contents keyed by SHA-1
 *  ├── branches/      -- files named by branch, containing commit SHA-1
 *  ├── remotes/       -- files named by remote, containing remote directory path
 *  ├── HEAD           -- name of the current branch
 *  ├── addstage       -- stage for addition, serialized TreeMap<String,String> (filename -> blob SHA-1)
 *  └── rmstage        -- stage for removal, serialized TreeSet<String> (filenames staged for removal)
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    /** Directory storing serialized Commit objects. */
    static final File COMMITS_DIR = Utils.join(GITLET_DIR, "commits");
    /** Directory storing blob (file content) objects. */
    static final File BLOBS_DIR = Utils.join(GITLET_DIR, "blobs");
    /** Directory storing branch head pointers. */
    static final File BRANCHES_DIR = Utils.join(GITLET_DIR, "branches");
    /** Directory storing remote information. */
    static final File REMOTES_DIR = Utils.join(GITLET_DIR, "remotes");
    /** File storing the current branch name (HEAD). */
    static final File HEAD_FILE = Utils.join(GITLET_DIR, "HEAD");
    /** File storing the staging area for additions. */
    static final File ADD_STAGE_FILE = Utils.join(GITLET_DIR, "addstage");
    /** File storing the staging area for removals. */
    static final File RM_STAGE_FILE = Utils.join(GITLET_DIR, "rmstage");

    /* TODO: fill in the rest of this class. */
    public static boolean isInitialized() {
        return GITLET_DIR.exists() && GITLET_DIR.isDirectory();
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * if .gitlet exists:
     *     print error
     * else:
     *     create .gitlet directories
     *     create initial commit
     *     save commit
     *     create branch master -> initialCommitId
     *     write HEAD = master
     *     save empty staging area */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        REMOTES_DIR.mkdir();

        // Create initial commit (epoch time, no files, no parent).
        Commit initial = new Commit();
        initial.saveCommit();

        // master branch -> initial commit; HEAD -> master
        setBranchCommitId("master", initial.getId());
        setHeadBranch("master");
        StagingArea stage = new StagingArea();
        stage.saveStage();
    }

    /** Adds a copy of the file to the staging area.
     * if file does not exist:
     *     print "File does not exist."
     *
     * read file bytes
     * blobId = sha1(file bytes)
     * currentCommit = head commit
     *
     * if file tracked in currentCommit and same blobId:
     *     remove file from addstage
     * else:
     *     save blob if not exists
     *     put < filename, blobId > into addstage
     *
     * remove file from rmstage if present
     * save stage */
    public static void add(String fileName) {
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        byte[] contents = Utils.readContents(file);
        String blobId = Utils.sha1((Object) contents);
        Commit curCommit = getHeadCommit();
        String curCommitBlobId = curCommit.getBlobId(fileName);
        StagingArea stage = StagingArea.readStage();

        if (curCommitBlobId != null && curCommitBlobId.equals(blobId)) {
            // Identical to HEAD version: unstage if staged.
            if (stage.isStagedForAddition(fileName)) stage.unstageAddition(fileName);
        } else {
            // Stage the file for addition.
            saveBlob(contents);
            stage.stageForAddition(fileName, blobId);
        }

        stage.unstageRemoval(fileName);
        stage.saveStage();
    }

    /** Saves a snapshot of tracked files in the current commit and staging area. */
    public static void commit(String message) {
        commitWith(message, null);
    }

    /** Internal commit, optionally with a second parent (used by merge).
     * if message empty:
     *     print "Please enter a commit message."
     *
     * if staging area is empty:
     *     print "No changes added to the commit."
     *
     * newBlobs = copy parent.blobs
     *
     * for each staged addition:
     *     newBlobs[file] = blobId
     *
     * for each staged removal:
     *     remove file from newBlobs
     *
     * create new commit with parent = current HEAD commit
     * save commit
     * update current branch pointer
     * clear staging area */
    private static void commitWith(String message, String secondParent) {
        StagingArea stage = StagingArea.readStage();
        TreeMap<String, String> addStage = stage.getAddStage();
        TreeSet<String> rmStage = stage.getRemoveStage();

        if (stage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit head = getHeadCommit();
        TreeMap<String, String> newBlobs = head.getBlobs();

        // Apply staged additions.
        for (Map.Entry<String, String> e : addStage.entrySet()) {
            newBlobs.put(e.getKey(), e.getValue());
        }
        // Apply staged removals.
        for (String file : rmStage) {
            newBlobs.remove(file);
        }

        Commit newCommit = new Commit(message, getHeadCommitId(), secondParent, newBlobs);
        String commitId = newCommit.saveCommit();

        // Advance current branch.
        setBranchCommitId(getHeadBranch(), commitId);
        stage.clearStage();
        stage.saveStage();
    }

    /** Unstages the file, or stages it for removal.
     * If the file is in the addstage:
     * Remove it from the addstage.
     *
     * If the file is tracked by the HEAD commit:
     * Add it to the rmstage.
     * If the file exists in the working directory, delete it.
     * (do not delete it unless it is tracked in the current commit)
     *
     * If neither of the above conditions is met, print:
     * No reason to remove the file. */
    public static void rm(String fileName) {
        StagingArea stage = StagingArea.readStage();
        Commit head = getHeadCommit();

        boolean staged = stage.isStagedForAddition(fileName);
        boolean tracked = head.containsFile(fileName);

        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (staged) {
            stage.unstageAddition(fileName);
        }
        if (tracked) {
            stage.stageForRemoval(fileName);
            File file = Utils.join(CWD, fileName);
            if (file.exists()) {
                Utils.restrictedDelete(file);
            }
        }

        stage.saveStage();
    }

    /** Displays commit history from HEAD backwards along the first-parent chain. */
    public static void log() {
        Commit head = getHeadCommit();
        while (head != null) {
            System.out.print(head.getLogEntry());
            head = Commit.readCommit(head.getParent());
        }
    }

    /** Displays information about all commits ever made. */
    public static void globalLog() {
        List<String> all = Utils.plainFilenamesIn(COMMITS_DIR);
        if (all == null) {
            return;
        }
        for (String id : all) {
            System.out.print(Commit.readCommit(id).getLogEntry());
        }
    }

    /** checkout -- [file name]: restores file from HEAD commit. */
    public static void checkoutFile(String fileName) {
        checkoutCommitFile(getHeadCommitId(), fileName);
    }

    /** checkout [commit id] -- [file name]: restores file from the commit with given ID. */
    public static void checkoutCommitFile(String shortId, String fileName) {
        String commitId = Commit.findCommitId(shortId);
        if (commitId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit c = Commit.readCommit(commitId);
        if (!c.containsFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        byte[] contents = readBlob(c.getBlobs().get(fileName));
        Utils.writeContents(Utils.join(CWD, fileName), (Object) contents);
    }

    /** checkout [branch name]: checks out all files in the given branch's head commit.
     * Check whether the branch exists.
     * Check whether it is the current branch.
     * Check for untracked file conflicts.
     *
     * Delete files tracked by the current commit but not tracked by the target commit.
     * Write out all files tracked by the target commit.
     *
     * Clear the staging area, unless the target branch is the current branch.
     * Make HEAD point to the target branch. */
    public static void checkoutBranch(String branchName) {
        if (!branchExists(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(getHeadBranch())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        List<String> untrackedFiles = getUntrackedFiles();
        Commit targetCommit = Commit.readCommit(getBranchCommitId(branchName));
        checkUntrackedConflict(untrackedFiles, targetCommit);

        // Delete files tracked by current commit but not by target commit.
        Commit curCommit = getHeadCommit();
        for (String file : curCommit.getBlobs().keySet()) {
            if (!targetCommit.containsFile(file)) {
                Utils.restrictedDelete(Utils.join(CWD, file));
            }
        }

        // Write all files tracked by target commit.
        String targetId = targetCommit.getId();
        for (String file : targetCommit.getBlobs().keySet()) {
            checkoutCommitFile(targetId, file);
        }

        setHeadBranch(branchName);
        StagingArea stage = StagingArea.readStage();
        stage.clearStage();
        stage.saveStage();
    }

    /** If a working file is untracked in the current branch and would be overwritten, print. */
    private static void checkUntrackedConflict(List<String> untrackedFiles, Commit targetCommit) {
        for (String file : untrackedFiles) {
            if (targetCommit.containsFile(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Prints ids of all commits with the given message. */
    public static void find(String message) {
        List<String> all = Utils.plainFilenamesIn(COMMITS_DIR);
        boolean found = false;
        if (all != null) {
            for (String id : all) {
                if (Commit.readCommit(id).getMessage().equals(message)) {
                    System.out.println(id);
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays branches, staged files, removed files, modifications, and untracked. */
    public static void status() {
        StagingArea stage = StagingArea.readStage();
        TreeMap<String, String> addStage = stage.getAddStage();
        TreeSet<String> rmStage = stage.getRemoveStage();
        Commit head = getHeadCommit();
        String headBranch = getHeadBranch();

        // === Branches ===
        System.out.println("=== Branches ===");
        List<String> branches = getAllBranchNames();
        if (branches != null) {
            for (String b : branches) {
                System.out.println(b.equals(headBranch) ? "*" + b : b);
            }
        }
        System.out.println();

        // === Staged Files ===
        System.out.println("=== Staged Files ===");
        for (String f : addStage.keySet()) {
            System.out.println(f);
        }
        System.out.println();

        // === Removed Files ===
        System.out.println("=== Removed Files ===");
        for (String f : rmStage) {
            System.out.println(f);
        }
        System.out.println();

        // === Modifications Not Staged For Commit === (extra credit)
        System.out.println("=== Modifications Not Staged For Commit ===");
        TreeMap<String, String> mods = getModifiedNotStaged(head, stage);
        for (Map.Entry<String, String> e : mods.entrySet()) {
            System.out.println(e.getKey() + " " + e.getValue());
        }
        System.out.println();

        // === Untracked Files === (extra credit)
        System.out.println("=== Untracked Files ===");
        List<String> untrackedFiles = getUntrackedFiles();
        for (String f : untrackedFiles) {
            System.out.println(f);
        }
        System.out.println();
    }

    /** Creates a new branch pointing at the current HEAD commit. */
    public static void branch(String branchName) {
        if (branchExists(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        setBranchCommitId(branchName, getHeadCommitId());
    }

    /** Deletes the branch with the given name. */
    public static void rmBranch(String branchName) {
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        if (!branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(getHeadBranch())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branchFile.delete();
    }

    /** Checks out all files tracked by the given commit; moves current branch head.
     * Check whether the commit exists.
     * Check for untracked file conflicts.
     *
     * Restore the working directory to the snapshot of the target commit.
     *
     * Make the current branch point to the target commit.
     * Clear the staging area. */
    public static void reset(String shortId) {
        String targetId = Commit.findCommitId(shortId);
        if (targetId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        List<String> untrackedFiles = getUntrackedFiles();
        Commit targetCommit = Commit.readCommit(targetId);
        checkUntrackedConflict(untrackedFiles, targetCommit);

        // Delete files tracked by current commit but not by target commit.
        Commit curCommit = getHeadCommit();
        for (String file : curCommit.getBlobs().keySet()) {
            if (!targetCommit.containsFile(file)) {
                Utils.restrictedDelete(Utils.join(CWD, file));
            }
        }

        // Write all files tracked by target commit.
        for (String file : targetCommit.getBlobs().keySet()) {
            checkoutCommitFile(targetId, file);
        }

        setBranchCommitId(getHeadBranch(), targetId);
        StagingArea stage = StagingArea.readStage();
        stage.clearStage();
        stage.saveStage();
    }

    /** Merges files from the given branch into the current branch.
     * Check whether the staging area is empty.
     * Check whether branchName exists.
     * Check whether branchName is the current branch.
     * Check for untracked file conflicts.
     *
     * Find the split point.
     * Handle the fast-forward / ancestor cases.
     * Perform a three-way comparison on all relevant files:
     * Create a merge commit whose second parent is the given branch head
     * Print if there is a merge conflict*/
    public static void merge(String branchName) {
        // ---- failure checks ----
        StagingArea stage = StagingArea.readStage();
        String currentId = getHeadCommitId();
        String givenId = getBranchCommitId(branchName);

        if (!stage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (givenId == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(getHeadBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        // Check for untracked files that would be overwritten.
        List<String> untrackedFiles = getUntrackedFiles();
        Commit targetCommit = Commit.readCommit(getBranchCommitId(branchName));
        checkUntrackedConflict(untrackedFiles, targetCommit);

        // ---- find split point ----
        String splitId = findSplitPoint(currentId, givenId);

        // ---- special cases ----
        if (splitId.equals(givenId)) {
            System.out.println("Given branch is an ancestor of the "
                    + "current branch.");
            return;
        }
        if (splitId.equals(currentId)) {
            // Fast-forward: move current branch to given branch's commit.
            reset(givenId);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // ---- normal merge ----
        Commit splitCommit = Commit.readCommit(splitId);
        Commit currentCommit = getHeadCommit();
        boolean conflict = performMerge(splitCommit, currentCommit, targetCommit);

        // Merge commit.
        String msg = "Merged " + branchName + " into " + getHeadBranch() + ".";
        commitWith(msg, givenId);

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Performs the file-level merge between split, current, and given commits.
     *  Writes files to CWD and updates the staging area.
     *  Returns true if any conflict was encountered.
     *      given               current             split       operation
     *  1.  mod                 no-mod              exist       checkout given and stage
     *  2.  no-mod              mod                 exist       continue
     *  3.  mod                 mod                             same, continue
     *  4.  no-mod[no-exist]    mod[exist]          no-exist    continue
     *  5.  mod[exist]          no-mod[no-exist]    no-exist    checkout given and stage
     *  6.  mod[no-exist]       no-mod              exist       remove and untrack
     *  7.  no-mod              mod[no-exist]       exist       continue
     *  8.  mod                 mod                             conflict, replace and stage
     *  */
    private static boolean performMerge(Commit split, Commit current, Commit given) {
        StagingArea stage = StagingArea.readStage();
        boolean conflict = false;

        // Collect all file names across the three commits.
        Set<String> allFiles = new TreeSet<>();
        allFiles.addAll(split.getBlobs().keySet());
        allFiles.addAll(current.getBlobs().keySet());
        allFiles.addAll(given.getBlobs().keySet());

        for (String file : allFiles) {
            String splitBlob = split.getBlobs().get(file);
            String currBlob = current.getBlobs().get(file);
            String givenBlob = given.getBlobs().get(file);

            boolean modInCurr = !Objects.equals(splitBlob, currBlob);
            boolean modInGiven = !Objects.equals(splitBlob, givenBlob);

            if (modInGiven && !modInCurr) {
                // Modified only in given branch.
                if (givenBlob != null) {
                    // case 1, case 5: checkout from given & stage.
                    checkoutCommitFile(given.getId(), file);
                    stage.stageForAddition(file, given.getBlobId(file));
                } else {
                    // case 6: remove & stage for removal.
                    File f = Utils.join(CWD, file);
                    if (f.exists()) {
                        Utils.restrictedDelete(f);
                    }
                    stage.stageForRemoval(file);
                }
            } else if (!modInGiven && modInCurr) {
                // Modified only in current branch — keep as is (case 2, case 7, case 4).
                continue;
            } else {
                // Modified in both branches.
                if (Objects.equals(currBlob, givenBlob)) {
                    // case 3: same modification — do nothing.
                    continue;
                } else {
                    // case 8: conflict.
                    conflict = true;
                    // construct the contents
                    String currContents = currBlob == null ? "" : new String(readBlob(currBlob));
                    String givenContents = givenBlob == null ? "" : new String(readBlob(givenBlob));
                    String merged = "<<<<<<< HEAD\n"
                            + currContents
                            + "=======\n"
                            + givenContents
                            + ">>>>>>>\n";
                    // replace the contents (file and blob)
                    Utils.writeContents(Utils.join(CWD, file), merged);
                    String newBlobId = saveBlob(merged.getBytes());
                    stage.stageForAddition(file, newBlobId);
                }
            }
        }

        stage.saveStage();
        return conflict;
    }

    /** Saves remote name -> directory mapping. */
    public static void addRemote(String remoteName, String remoteDir) {
        File remoteFile = Utils.join(REMOTES_DIR, remoteName);
        if (remoteFile.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        // Convert forward slashes to platform separator.
        String dirPath = remoteDir.replace("/", File.separator);
        Utils.writeContents(remoteFile, dirPath);
    }

    /** Removes the remote with the given name. */
    public static void rmRemote(String remoteName) {
        File remoteFile = Utils.join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        remoteFile.delete();
    }

    /** Pushes current branch's commits to the remote branch.
     * check whether .gitlet exists in remote
     * check whether remote branch head is in the history of current local head
     *
     * if remote branch head is in the history of current local head, fast-forward
     * if .gitlet exists in remote but remote branches-directory doesnt have local head, add to remote*/
    public static void push(String remoteName, String remoteBranchName) {
        // Check whether .gitlet exists
        File remoteGitlet = getRemoteGitletDir(remoteName);
        if (remoteGitlet == null || !remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }

        // Get remote branch head (if exists).
        File remoteBranchesDir = Utils.join(remoteGitlet, "branches");
        File remoteBranchFile = Utils.join(remoteBranchesDir, remoteBranchName);
        String remoteHeadId = null;
        if (remoteBranchFile.exists()) {
            remoteHeadId = Utils.readContentsAsString(remoteBranchFile).trim();
        }

        // Check whether remote head is in local history.
        String localHeadId = getHeadCommitId();
        if (remoteHeadId != null) {
            Map<String, Integer> localAncestors = getAncestorDistances(localHeadId);
            if (!localAncestors.containsKey(remoteHeadId)) {
                System.out.println("Please pull down remote changes before pushing.");
                System.exit(0);
            }
        }

        List<String> toPush = collectCommitsToPush(remoteName, localHeadId, remoteHeadId);

        // Copy commits and their blobs to remote.
        File remoteCommitsDir = Utils.join(remoteGitlet, "commits");
        File remoteBlobsDir = Utils.join(remoteGitlet, "blobs");
        for (String cid : toPush) {
            Commit c = Commit.readCommit(cid);
            // Copy commit.
            File remoteCommitFile = Utils.join(remoteCommitsDir, cid);
            if (!remoteCommitFile.exists()) {
                Utils.writeObject(remoteCommitFile, c);
            }
            // Copy blobs.
            for (String blobId : c.getBlobs().values()) {
                File remoteBlobFile = Utils.join(remoteBlobsDir, blobId);
                if (!remoteBlobFile.exists()) {
                    Utils.writeContents(remoteBlobFile,
                            (Object) readBlob(blobId));
                }
            }
        }

        // Reset remote branch to local head.
        setRemoteBranchCommitId(remoteName, remoteBranchName, localHeadId);
    }

    /** Collect commits to push (from local head back to remote head). */
    private static List<String> collectCommitsToPush(String remoteName, String localHeadId, String remoteHeadId) {
        File remoteGitlet = getRemoteGitletDir(remoteName);
        File remoteCommitsDir = Utils.join(remoteGitlet, "commits");
        Map<String, Integer> localAncestors = getAncestorDistances(localHeadId);
        Map<String, Integer> remoteAncestors = getAncestorDistances(remoteHeadId, remoteCommitsDir);
        List<String> toPush = new ArrayList<>();

        if (remoteHeadId == null){
            for (String commitId : localAncestors.keySet()){
                File remoteCommitFile = Utils.join(remoteCommitsDir, commitId);
                if (!remoteCommitFile.exists()){
                    toPush.add(commitId);
                }
            }
            return toPush;
        }

        for (String commitId : localAncestors.keySet()){
            if (!remoteAncestors.containsKey(commitId)){
                toPush.add(commitId);
            }
        }

        return toPush;
    }

    /** Fetches commits from the remote branch into a local tracking branch.
     * check whether .gitlet exists in remote
     * check whether remote branches-directory has given branch name
     *
     * copy all commits and blobs from given branch in remote (not exists in current)
     * into [remote name]/[remote branch name] in local branch
     *
     * change [remote name]/[remote branch name] to point to the head commit*/
    public static void fetch(String remoteName, String remoteBranchName) {
        File remoteGitlet = getRemoteGitletDir(remoteName);
        if (remoteGitlet == null || !remoteGitlet.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteBranchesDir = Utils.join(remoteGitlet, "branches");
        File remoteBranchFile = Utils.join(remoteBranchesDir, remoteBranchName);
        if (!remoteBranchFile.exists()) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }

        File remoteCommitsDir = Utils.join(remoteGitlet, "commits");
        File remoteBlobsDir = Utils.join(remoteGitlet, "blobs");
        String remoteHeadId = Utils.readContentsAsString(remoteBranchFile).trim();
        Map<String, Integer> remoteAncestors = getAncestorDistances(remoteHeadId, remoteCommitsDir);

        for (String commitId : remoteAncestors.keySet()){
            File localCommitFile = Utils.join(COMMITS_DIR, commitId);
            if (localCommitFile.exists()){
                continue;
            }
            // read and copy commit
            Commit c = Utils.readObject(Utils.join(remoteCommitsDir, commitId), Commit.class);
            Utils.writeObject(localCommitFile, c);
            // read and copy blobs
            for (String blobId : c.getBlobs().values()) {
                File localBlobFile = Utils.join(BLOBS_DIR, blobId);
                if (!localBlobFile.exists()) {
                    Utils.writeContents(localBlobFile,
                            (Object) readBlob(blobId, remoteBlobsDir));
                }
            }
        }

        // Create / update local tracking branch: remoteName/remoteBranchName
        String localTrackingBranch = remoteName + "/" + remoteBranchName;
        setBranchCommitId(localTrackingBranch, remoteHeadId);
    }

    /** Fetches and then merges the remote branch into the current branch. */
    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        String localTrackingBranch = remoteName + "/" + remoteBranchName;
        merge(localTrackingBranch);
    }

    /* ======================== PERSISTENCE HELPERS ======================== */

    /** Returns the current branch name from HEAD. */
    private static String getHeadBranch() {
        return Utils.readContentsAsString(HEAD_FILE).trim();
    }

    /** Sets HEAD to point to the specified branch name. */
    private static void setHeadBranch(String branchName){
        Utils.writeContents(HEAD_FILE, branchName);
    }

    /** Returns the commit ID that the current branch points to. */
    private static String getHeadCommitId() {
        return getBranchCommitId(getHeadBranch());
    }

    /** Returns the Commit object at the HEAD. */
    private static Commit getHeadCommit() {
        return Commit.readCommit(getHeadCommitId());
    }

    /** Returns the commit ID that a branch points to (null if branch doesn't exist). */
    private static String getBranchCommitId(String branchName) {
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        if (!branchFile.exists() || !branchFile.isFile()) {
            return null;
        }
        return Utils.readContentsAsString(branchFile).trim();
    }

    /** Sets a branch to point to the given commit ID. */
    private static void setBranchCommitId(String branchName, String commitId){
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        File parent = branchFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Utils.writeContents(branchFile, commitId);
    }

    /** Returns true if branchName exists as a branch file. */
    private static boolean branchExists(String branchName) {
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        return branchFile.exists() && branchFile.isFile();
    }


    /** Sets a remote branch to point to the given commit ID. */
    private static void setRemoteBranchCommitId(String remoteName, String remoteBranchName, String remoteCommitId){
        File remoteGitlet = getRemoteGitletDir(remoteName);
        File remoteBranchesDir = Utils.join(remoteGitlet, "branches");
        File remoteBranchFile = Utils.join(remoteBranchesDir, remoteBranchName);
        Utils.writeContents(remoteBranchFile, remoteCommitId);
    }

    /** Saves blob contents to disk and returns the SHA-1 id. */
    private static String saveBlob(byte[] contents){
        String blobId = Utils.sha1((Object) contents);
        File blobFile = Utils.join(BLOBS_DIR, blobId);
        if (!blobFile.exists()) {
            Utils.writeContents(blobFile, (Object) contents);
        }
        return blobId;
    }

    /** Reads and returns blob (raw file contents) with the given ID. */
    private static byte[] readBlob(String blobId){
        return readBlob(blobId, BLOBS_DIR);
    }

    /** Reads and returns blob (raw file contents) with the given ID and given dir. */
    private static byte[] readBlob(String blobId, File blobsDir){
        return Utils.readContents(Utils.join(blobsDir, blobId));
    }

    /** Returns a sorted list of file names in CWD that are untracked.
     *  Untracked = in CWD but neither staged for addition nor tracked by HEAD,
     *  OR staged for removal but re-created in CWD (command rm). */
    private static List<String> getUntrackedFiles() {
        List<String> result = new ArrayList<>();
        Commit head = getHeadCommit();
        StagingArea stage = StagingArea.readStage();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);

        if (cwdFiles == null) {
            return result;
        }
        for (String file : cwdFiles) {
            boolean tracked = head.containsFile(file);
            boolean staged = stage.isStagedForAddition(file);
            boolean removed = stage.isStagedForRemoval(file);
            if ((!staged && !tracked) || removed) {
                result.add(file);
            }
        }
        Collections.sort(result);
        return result;
    }

    /** Returns filename->(modified)/(deleted) for files modified but not staged.
     * CWD      HEAD    addStage    rmStage
     * modify   y       n           n
     * modify   -       y           n
     * delete   -       y           n
     * delete   y       -/n         n*/
    private static TreeMap<String, String> getModifiedNotStaged(
            Commit head, StagingArea stage) {
        TreeMap<String, String> result = new TreeMap<>();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        Set<String> cwdSet = new TreeSet<>();
        if (cwdFiles != null) {
            cwdSet.addAll(cwdFiles);
        }

        // Check files tracked in HEAD.
        for (Map.Entry<String, String> e : head.getBlobs().entrySet()) {
            String name = e.getKey();
            String blobId = e.getValue();
            if (cwdSet.contains(name)) {
                String cur = Utils.sha1(
                        (Object) Utils.readContents(Utils.join(CWD, name)));
                if (!cur.equals(blobId) && !stage.isStagedForAddition(name)) {
                    result.put(name, "(modified)");
                }
            } else if (!stage.isStagedForRemoval(name) && !stage.isStagedForAddition(name)) {
                result.put(name, "(deleted)");
            }
        }

        // Check files staged for addition.
        for (Map.Entry<String, String> e : stage.getAddStage().entrySet()) {
            String name = e.getKey();
            String blobId = e.getValue();
            if (cwdSet.contains(name)) {
                String cur = Utils.sha1(
                        (Object) Utils.readContents(Utils.join(CWD, name)));
                if (!cur.equals(blobId)) {
                    result.put(name, "(modified)");
                }
            } else {
                result.put(name, "(deleted)");
            }
        }
        return result;
    }

    /**
     * Returns the SHA-1 of the latest common ancestor (split point).
     * When multiple common ancestors exist at the same distance from
     * currentId, picks the one closest to givenId, ensuring a
     * deterministic result.
     */
    private static String findSplitPoint(String currentId, String givenId) {
        Map<String, Integer> currentDist = getAncestorDistances(currentId);
        Map<String, Integer> givenDist = getAncestorDistances(givenId);

        String best = null;
        int bestMax = Integer.MAX_VALUE;
        int bestGiven = Integer.MAX_VALUE;

        for (String id : currentDist.keySet()) {
            if (!givenDist.containsKey(id)) {
                continue;
            }
            int dc = currentDist.get(id);
            int dg = givenDist.get(id);
            int maxD = Math.max(dc, dg);

            // Prefer smallest max-distance; break ties by smallest given-distance;
            // final tiebreak by hash for full determinism.
            if (maxD < bestMax
                    || (maxD == bestMax && dg < bestGiven)
                    || (maxD == bestMax && dg == bestGiven
                    && id.compareTo(best) < 0)) {
                best = id;
                bestMax = maxD;
                bestGiven = dg;
            }
        }
        return best;
    }

    /** Returns the map of all ancestor commit ids (inclusive) reachable from commitId. */
    private static Map<String, Integer> getAncestorDistances(String commitId) {
        return getAncestorDistances(commitId, Repository.COMMITS_DIR);
    }

    /** Returns the map of all ancestor commit ids (inclusive) reachable from commitId and commitsDir. */
    private static Map<String, Integer> getAncestorDistances(String commitId, File commitsDir) {
        Map<String, Integer> ancestors = new HashMap<>();
        if (commitId == null) return ancestors;

        Queue<String> queue = new ArrayDeque<>();
        queue.add(commitId);
        ancestors.put(commitId, 0);

        while (!queue.isEmpty()) {
            String id = queue.poll();
            int dist = ancestors.get(id);
            Commit c = Commit.readCommit(id, commitsDir); // ← 传入目录

            if (c == null) continue;

            String parent = c.getParent();
            if (parent != null && !ancestors.containsKey(parent)) {
                ancestors.put(parent, dist + 1);
                queue.add(parent);
            }

            String secondParent = c.getSecondParent();
            if (secondParent != null && !ancestors.containsKey(secondParent)) {
                ancestors.put(secondParent, dist + 1);
                queue.add(secondParent);
            }
        }
        return ancestors;
    }

    /** Returns the File of the remote .gitlet directory for the given remote name. */
    private static File getRemoteGitletDir(String remoteName) {
        File remoteFile = Utils.join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()){
            return null;
        }
        String dirPath = Utils.readContentsAsString(remoteFile).trim();
        return new File(dirPath);
    }

    /** Returns all branch names, including one-level remote branches like origin/master. */
    private static List<String> getAllBranchNames() {
        List<String> result = new ArrayList<>();

        File[] entries = BRANCHES_DIR.listFiles();
        if (entries == null) {
            return result;
        }

        result.addAll(Utils.plainFilenamesIn(BRANCHES_DIR));
        for (File entry : entries) {
            if (entry.isDirectory()) {
                String dirName = entry.getName();
                List<String> children = Utils.plainFilenamesIn(entry);
                if (children != null) {
                    for (String child : children) {
                        result.add(dirName + "/" + child);
                    }
                }
            }
        }

        Collections.sort(result);
        return result;
    }
}
