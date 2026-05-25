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
            head.getLogEntry();
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
            Commit.readCommit(id).getLogEntry();
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
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(getHeadBranch())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        List<String> untrackedFiles = getUntrackedFiles();
        Commit targetCommit = Commit.readCommit(getBranchCommitId(branchName));
        for (String file : untrackedFiles) {
            if (targetCommit.containsFile(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }

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

        new StagingArea().clearStage();
        setHeadBranch(branchName);
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
        if (!branchFile.exists()) {
            return null;
        }
        return Utils.readContentsAsString(branchFile).trim();
    }

    /** Sets a branch to point to the given commit ID. */
    private static void setBranchCommitId(String branchName, String commitId){
        File branchFile = Utils.join(BRANCHES_DIR, branchName);
        Utils.writeContents(branchFile, commitId);
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
        return Utils.readContents(Utils.join(BLOBS_DIR, blobId));
    }

    /** Write the content of a blob to the specified file. */
    private static void writeBlobToWorkingFile(String blobId, String fileName){
        // TODO
    }

    /** Returns true if the blob exists with the given ID. */
    private static boolean blobExists(String blobId){
        // TODO
        return true;
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
            if (!staged && !tracked) {
                result.add(file);
            } else if (stage.isStagedForRemoval(file) && !staged) {
                result.add(file);
            }
        }
        Collections.sort(result);
        return result;
    }
}
