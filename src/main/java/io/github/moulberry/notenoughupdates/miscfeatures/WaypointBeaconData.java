package io.github.moulberry.notenoughupdates.miscfeatures;

import net.minecraft.util.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class WaypointBeaconData {
    public Boolean enabled = false;
    public String singularName = "UNDEFINED";
    public String pluralName = "UNDEFINED";
    public HashMap<String, Set<Integer>> foundWaypoints = new HashMap<>();
    public List<BlockPos> currentWaypointList = null;
    public List<BlockPos> currentWaypointListClose = null;

    private WaypointBeaconData() {}

    public WaypointBeaconData(String singularName, String pluralName) {
        this.singularName = singularName;
        this.pluralName = pluralName;
    }
}
