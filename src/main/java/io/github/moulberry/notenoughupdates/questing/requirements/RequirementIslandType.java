package io.github.moulberry.notenoughupdates.questing.requirements;

import io.github.moulberry.notenoughupdates.questing.NEUQuesting;

public class RequirementIslandType extends Requirement {

    private String islandType;

    public RequirementIslandType(String islandType, Requirement... preconditions) {
        super(preconditions);
        this.islandType = islandType;
    }


    @Override
    public void updateRequirement() {
        completed = islandType.equalsIgnoreCase(NEUQuesting.getInstance().location);
    }
}
