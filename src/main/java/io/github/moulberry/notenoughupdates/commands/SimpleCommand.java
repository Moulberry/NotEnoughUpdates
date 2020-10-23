package io.github.moulberry.notenoughupdates.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SimpleCommand extends CommandBase {

    private String commandName;
    private ProcessCommandRunnable runnable;
    private TabCompleteRunnable tabRunnable;

    public SimpleCommand(String commandName, ProcessCommandRunnable runnable) {
        this.commandName = commandName;
        this.runnable = runnable;
    }

    public SimpleCommand(String commandName, ProcessCommandRunnable runnable, TabCompleteRunnable tabRunnable) {
        this.commandName = commandName;
        this.runnable = runnable;
        this.tabRunnable = tabRunnable;
    }

    public abstract static class ProcessCommandRunnable {
        public abstract void processCommand(ICommandSender sender, String[] args);
    }

    public abstract static class TabCompleteRunnable {
        public abstract List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos);
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

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if(tabRunnable != null) return tabRunnable.tabComplete(sender, args, pos);
        return null;
    }
}
