package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemCooldowns;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public class MixinItemStack {

    @Inject(method="hasEffect", at=@At("HEAD"), cancellable = true)
    public void hasEffect(CallbackInfoReturnable cir) {
        if(Utils.getHasEffectOverride()) {
            cir.setReturnValue(false);
        }
    }

    @Shadow
    private NBTTagCompound stackTagCompound;

    @Inject(method="getDisplayName",at=@At("HEAD"), cancellable=true)
    public void getDisplayName(CallbackInfoReturnable<String> returnable) {
        try {
            if(stackTagCompound == null || !stackTagCompound.hasKey("ExtraAttributes", 10)) {
                return;
            }

            String customName = NotEnoughUpdates.INSTANCE.manager.itemRenameJson
                    .get(stackTagCompound.getCompoundTag("ExtraAttributes").getString("uuid")).getAsString();
            if(customName != null && !customName.equals("")) {
                String prefix = EnumChatFormatting.RESET.toString();
                if (stackTagCompound != null && stackTagCompound.hasKey("display", 10)) {
                    NBTTagCompound nbttagcompound = stackTagCompound.getCompoundTag("display");

                    if (nbttagcompound.hasKey("Name", 8)) {
                        String name = nbttagcompound.getString("Name");
                        char[] chars = name.toCharArray();

                        int i;
                        for(i=0; i<chars.length; i+=2) {
                            if(chars[i] != '\u00a7'){
                                break;
                            }
                        }

                        prefix = name.substring(0, i);
                    }
                }
                returnable.setReturnValue(prefix+customName);
            }
        } catch(Exception e) { }
    }


}
