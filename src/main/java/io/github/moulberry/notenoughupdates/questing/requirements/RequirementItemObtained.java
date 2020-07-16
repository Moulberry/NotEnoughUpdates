package io.github.moulberry.notenoughupdates.questing.requirements;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;

import java.util.ArrayList;
import java.util.Map;

public class RequirementItemObtained extends Requirement {

    private String internalname;

    public RequirementItemObtained(String internalname, Requirement... preconditions) {
        super(preconditions);
        this.internalname = internalname;
    }

    private Map<String, ArrayList<String>> getAcquiredItems() {
        return NotEnoughUpdates.INSTANCE.manager.config.collectionLog.value;
    }

    @Override
    public void updateRequirement() {
        if(getAcquiredItems() != null &&
                getAcquiredItems().containsKey(NotEnoughUpdates.INSTANCE.manager.getCurrentProfile()) &&
                getAcquiredItems().get(NotEnoughUpdates.INSTANCE.manager.getCurrentProfile()).contains(internalname)) {
            completed = true;
        }
    }
}
