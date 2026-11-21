package branchforge;

import branchforge.command.Command;
import branchforge.command.CommandRegistry;
import branchforge.command.InitCommand;
import branchforge.repo.Repository;

import java.io.File;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            fail("Please enter a command.");
        }

        String commandName = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
        File rootDir = new File(System.getProperty("user.dir"));

        try {
            run(rootDir, commandName, commandArgs);
        } catch (BranchForgeException e) {
            fail(e.getMessage());
        } catch (RuntimeException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    private static void run(File rootDir, String commandName, String[] commandArgs) {
        if ("init".equals(commandName)) {
            new InitCommand(rootDir).execute(commandArgs);
            return;
        }

        if (!CommandRegistry.isKnown(commandName)) {
            throw new BranchForgeException("No command with that name exists.");
        }

        Repository repo = Repository.load(rootDir);
        Command command = new CommandRegistry(repo).get(commandName);
        command.execute(commandArgs);

        repo.persist();
    }

    private static void fail(String message) {
        System.out.println(message);
        System.exit(0);
    }
}