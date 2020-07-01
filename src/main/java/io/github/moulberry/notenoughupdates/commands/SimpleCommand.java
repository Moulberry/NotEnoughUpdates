package io.github.moulberry.notenoughupdates.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class SimpleCommand extends CommandBase {

    private String commandName;
    private ProcessCommandRunnable runnable;

    public SimpleCommand(String commandName, ProcessCommandRunnable runnable) {
        this.commandName = commandName;
        this.runnable = runnable;
    }

    public abstract static class ProcessCommandRunnable {
        public abstract void processCommand(ICommandSender sender, String[] args);
    }

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/" + commandName;
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        runnable.processCommand(sender, args);
    }
}
