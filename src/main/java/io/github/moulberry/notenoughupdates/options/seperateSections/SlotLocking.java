package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorKeybind;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorSlider;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class SlotLocking {
    @Expose
    @ConfigOption(
            name = "Enable Slot Locking",
            desc = "Allows you to lock slots and create slot bindings"
    )
    @ConfigEditorBoolean
    public boolean enableSlotLocking = false;

    @Expose
    @ConfigOption(
            name = "Enable Slot Binding",
            desc = "Allows you to create slot bindings\nNote: \"Enable Slot Locking\" must be on"
    )
    @ConfigEditorBoolean
    public boolean enableSlotBinding = true;

    @Expose
    @ConfigOption(
            name = "Don't Drop Bound Slots",
            desc = "Slot bindings also act as locked slots (prevents dropping / moving in inventory)"
    )
    @ConfigEditorBoolean
    public boolean bindingAlsoLocks = false;

    @Expose
    @ConfigOption(
            name = "Slot Lock Key",
            desc = "Click this key to LOCK a slot\n" +
                    "Hold this key and drag to BIND a slot"
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_L)
    public int slotLockKey = Keyboard.KEY_L;

    @Expose
    @ConfigOption(
            name = "Lock Slots in Trade",
            desc = "Prevents trading locked items in the custom trade windows"
    )
    @ConfigEditorBoolean
    public boolean lockSlotsInTrade = true;

    @Expose
    @ConfigOption(
            name = "Slot Lock Sound",
            desc = "Play a ding when locking/unlocking slots"
    )
    @ConfigEditorBoolean
    public boolean slotLockSound = true;

    @Expose
    @ConfigOption(
            name = "Slot Lock Sound Vol.",
            desc = "Set the volume of the ding sound"
    )
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 100,
            minStep = 1
    )
    public float slotLockSoundVol = 20;
}
