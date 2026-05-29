package gitlet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList(
            "init", "add", "commit", "rm", "log", "global-log", "find",
            "status", "checkout", "branch", "rm-branch", "reset", "merge",
            "add-remote", "rm-remote", "push", "fetch", "pull"
    ));

    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];

        if (!COMMANDS.contains(firstArg)) {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
        if (!firstArg.equals("init") && !Repository.isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                validateNumArgs(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length < 2 || args[1].trim().isEmpty()) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateNumArgs(args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validateNumArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                validateNumArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validateNumArgs(args, 1);
                Repository.status();
                break;
            case "checkout":
                if (args.length == 3 && args[1].equals("--")) {
                    // java gitlet.Main checkout -- [file name]
                    Repository.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    // java gitlet.Main checkout [commit id] -- [file name]
                    Repository.checkoutCommitFile(args[1], args[3]);
                } else if (args.length == 2) {
                    // java gitlet.Main checkout [branch name]
                    Repository.checkoutBranch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                validateNumArgs(args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs(args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                validateNumArgs(args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validateNumArgs(args, 2);
                Repository.merge(args[1]);
                break;
            case "add-remote":
                validateNumArgs(args, 3);
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validateNumArgs(args, 2);
                Repository.rmRemote(args[1]);
                break;
            case "push":
                validateNumArgs(args, 3);
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                validateNumArgs(args, 3);
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                validateNumArgs(args, 3);
                Repository.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * prints "Incorrect operands." and exits if they do not match.
     *
     * @param args Argument array from command line
     * @param n    Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
