package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.List;
import java.util.TreeMap;

/**
 * Represents a gitlet commit object.
 * A Commit is a snapshot of tracked files at a point in time.
 * It contains:
 *   - A log message
 *   - A timestamp
 *   - A mapping of file names to blob references (SHA-1 hashes)
 *   - A parent commit reference (SHA-1 hash)
 *   - An optional second parent for merge commits
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    /** The timestamp of this Commit. */
    private final Date timestamp;
    /** The SHA-1 hash of the first parent commit. Null for the initial commit. */
    private final String parent;
    /** The SHA-1 hash of the second parent commit (only for merge commits). */
    private final String secondParent;
    /**
     * A mapping from file names to their blob ID (SHA-1 hash).
     * This represents the snapshot of all tracked files at this commit.
     */
    private final TreeMap<String, String> blobs;
    /** SHA-1 hash of this commit. */
    private transient String cachedId;

    /* TODO: fill in the rest of this class. */
    /**
     * Creates the initial commit.
     * The initial commit has no parent, no tracked files,
     * and uses the Unix epoch as its timestamp.
     */
    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0); // Unix epoch: Thu Jan 1 00:00:00 1970
        this.parent = null;
        this.secondParent = null;
        this.blobs = new TreeMap<>();
    }

    /**
     * Creates a new commit with the given message and parent.
     * Inherits the file snapshot (blobs) from its parent commit.
     *
     * @param message the commit message
     * @param parent  the SHA-1 hash of the parent commit
     * @param blobs   the file-to-blob mapping inherited from the parent
     */
    public Commit(String message, String parent, TreeMap<String, String> blobs) {
        this.message = message;
        this.timestamp = new Date(); // current time
        this.parent = parent;
        this.secondParent = null;
        this.blobs = new TreeMap<>(blobs);
    }

    /**
     * Creates a merge commit with two parents.
     *
     * @param message      the commit message
     * @param parent       the SHA-1 hash of the first parent (current branch)
     * @param secondParent the SHA-1 hash of the second parent (merged-in branch)
     * @param blobs        the file-to-blob mapping after merge resolution
     */
    public Commit(String message, String parent, String secondParent,
                  TreeMap<String, String> blobs) {
        this.message = message;
        this.timestamp = new Date();
        this.parent = parent;
        this.secondParent = secondParent;
        this.blobs = new TreeMap<>(blobs);
    }

    /* ==================== Getters ==================== */

    /** Returns the commit message. */
    public String getMessage() {
        return message;
    }

    /** Returns the timestamp of this commit. */
    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    /** Returns the SHA-1 hash of the first parent commit. */
    public String getParent() {
        return parent;
    }

    /** Returns the SHA-1 hash of the second parent (null if not a merge). */
    public String getSecondParent() {
        return secondParent;
    }

    /** Returns the blob mapping (file name -> blob SHA-1). */
    public TreeMap<String, String> getBlobs() {
        return new TreeMap<>(blobs);
    }

    /**
     * Generates and returns the SHA-1 hash of this commit.
     * The hash is computed from the metadata(message, timestamp), parent references,
     * and the blob mapping, ensuring each unique commit state gets a unique ID.
     */
    public String getId() {
        if (cachedId == null) {
            cachedId = Utils.sha1((Object) Utils.serialize(this));
        }
        return cachedId;
    }

    /**
     * Returns the blob SHA-1 hash for the given file name,
     * or null if the file is not tracked.
     *
     * @param fileName the name of the file
     * @return the blob hash, or null
     */
    public String getBlobId(String fileName) {
        return blobs.get(fileName);
    }

    /* ==================== Core Methods ==================== */

    /**
     * Saves this commit to the objects directory in .gitlet.
     * The commit is serialized and stored under its SHA-1 hash.
     */
    public String saveCommit() {
        String commitId = getId();
        File commitFile = Utils.join(Repository.COMMITS_DIR, commitId);
        Utils.writeObject(commitFile, this);
        return commitId;
    }

    /**
     * Reads and returns the commit object with the given ID.
     *
     * @param commitId the SHA-1 hash of the commit
     * @return the Commit object, or null if not found
     */
    public static Commit readCommit(String commitId) {
        return readCommit(commitId, Repository.COMMITS_DIR);
    }

    /**
     * Reads and returns the commit object with the given ID and given dir.
     *
     * @param commitId the SHA-1 hash of the commit
     * @return the Commit object, or null if not found
     */
    public static Commit readCommit(String commitId, File commitsDir) {
        if (commitId == null) return null;
        File commitFile = Utils.join(commitsDir, commitId);
        if (!commitFile.exists()) return null;
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     * Checks whether this commit tracks a file with the given name.
     *
     * @param fileName the name of the file
     * @return true if the file is tracked by this commit
     */
    public boolean containsFile(String fileName) {
        return blobs.containsKey(fileName);
    }

    /**
     * Returns true if this is a merge commit (has two parents).
     */
    public boolean isMerge() {
        return parent != null && secondParent != null;
    }

    /**
     * Returns the log entry string for this commit, used by
     * the `log` and `global-log` commands.
     */
    public String getLogEntry() {
        StringBuilder sb = new StringBuilder();
        sb.append("===\n");
        sb.append("commit ").append(getId()).append("\n");
        if (isMerge()) {
            sb.append("Merge: ");
            sb.append(parent, 0, 7).append(" ");
            sb.append(secondParent, 0, 7).append("\n");
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy Z", java.util.Locale.ENGLISH);
        sb.append("Date: ").append(sdf.format(getTimestamp())).append("\n");
        sb.append(message).append("\n").append("\n");
        return sb.toString();
    }

    /** Returns the full commit ID based on the short ID */
    public static String findCommitId(String shortId){
        if (shortId == null || shortId.isEmpty()) return null;
        List<String> ids = Utils.plainFilenamesIn(Repository.COMMITS_DIR);
        if (ids == null) return null;

        String match = null;
        for (String id: ids){
            if (id.startsWith(shortId)){
                if (match != null) return null;
                match = id;
            }
        }
        return match;
    }
}
