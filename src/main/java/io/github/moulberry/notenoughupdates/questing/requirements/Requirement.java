package io.github.moulberry.notenoughupdates.questing.requirements;

public abstract class Requirement {

    private Requirement[] preconditions;

    protected boolean completed = false;

    public Requirement(Requirement... preconditions) {
        this.preconditions = preconditions;
    }

    public boolean getPreconditionCompleted() {
        boolean completed = true;
        for(Requirement precondition : preconditions) {
            completed &= precondition.completed;
        }
        return completed;
    }

    //Collection, Item obtained, GUI Open, Fake npc interact

    public boolean getCompleted() {
        return completed && getPreconditionCompleted();
    }

    public abstract void updateRequirement();

}
