package gitlet;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents the gitlet staging area.
 * Manages files staged for addition and files staged for removal.
 *
 * The staging area lives as 2 serialized files on disk
 * (.gitlet/addstage and .gitlet/rmstage).
 *
 * - Addition stage: maps filenames to their blob SHA-1 hashes.
 *   When a file is staged for addition, its current content is
 *   hashed and stored; at commit time these entries override or
 *   extend the parent commit's blob mapping.
 *
 * - Removal stage: a set of filenames marked for removal.
 *   At commit time these entries are excluded from the new
 *   commit's blob mapping.
 *
 * @author TODO
 */
public class StagingArea implements Serializable {

    /** Files staged for addition: filename -> blob id. */
    private final TreeMap<String, String> addStage;

    /** Files staged for removal. */
    private final TreeSet<String> rmStage;

    /** Creates an empty staging area. */
    public StagingArea() {
        addStage = new TreeMap<>();
        rmStage = new TreeSet<>();
    }

    public StagingArea(TreeMap<String, String> addStage, TreeSet<String> rmStage) {
        this.addStage = new TreeMap<>(addStage);
        this.rmStage = new TreeSet<>(rmStage);
    }

    /** Stages FILENAME with BLOBID for addition. */
    public void stageForAddition(String filename, String blobId) {
        // 1. Put filename -> blobId into addStage.
        // 2. If filename was staged for removal, unstage it from rmStage.
        addStage.put(filename, blobId);
        if (isStagedForRemoval(filename)) unstageAddition(filename);
    }

    /** Stages FILENAME for removal. */
    public void stageForRemoval(String filename) {
        // 1. Add filename to rmStage.
        // 2. If filename was staged for addition, remove it from addStage.
        rmStage.add(filename);
        if (isStagedForAddition(filename)) unstageRemoval(filename);
    }

    /** Unstages FILENAME from the addition stage. */
    public void unstageAddition(String filename) {
        addStage.remove(filename);
    }

    /** Unstages FILENAME from the removal stage. */
    public void unstageRemoval(String filename) {
        rmStage.remove(filename);
    }

    /** Returns true if FILENAME is staged for addition. */
    public boolean isStagedForAddition(String filename) {
        return addStage.containsKey(filename);
    }

    /** Returns true if FILENAME is staged for removal. */
    public boolean isStagedForRemoval(String filename) {
        return rmStage.contains(filename);
    }

    /** Returns a copy of the addition stage. */
    public TreeMap<String, String> getAddStage() {
        return new TreeMap<>(addStage);
    }

    /** Returns a copy of the removal stage. */
    public TreeSet<String> getRemoveStage() {
        return new TreeSet<>(rmStage);
    }

    /** Returns true if there are no staged additions or removals. */
    public boolean isEmpty() {
        return addStage.isEmpty() && rmStage.isEmpty();
    }

    /** Clears both addition and removal stages. */
    public void clearStage() {
        addStage.clear();
        rmStage.clear();
    }

    /** read from addstage and rmstage. */
    public static StagingArea readStage(){
        TreeMap<String, String> addStage = new TreeMap<>();
        TreeSet<String> rmStage = new TreeSet<>();

        if (Repository.ADD_STAGE_FILE.exists()) {
            addStage = Utils.readObject(Repository.ADD_STAGE_FILE, TreeMap.class);
        }
        if (Repository.RM_STAGE_FILE.exists()) {
            rmStage = Utils.readObject(Repository.RM_STAGE_FILE, TreeSet.class);
        }
        return new StagingArea(addStage, rmStage);
    }

    /** write to addstage and rmstage. */
    public void saveStage(){
        Utils.writeObject(Repository.ADD_STAGE_FILE, this.addStage);
        Utils.writeObject(Repository.RM_STAGE_FILE, this.rmStage);
    }
}
