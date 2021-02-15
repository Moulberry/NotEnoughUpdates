package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.regex.Pattern;

public class ItemCooldowns {

    private static Map<ItemStack, Float> durabilityOverrideMap = new HashMap<>();
    private static long pickaxeUseCooldownMillisRemaining = -1;
    private static long treecapitatorCooldownMillisRemaining = -1;
    private static long lastMillis = 0;

    public static TreeMap<Long, BlockPos> blocksClicked = new TreeMap<>();

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            long currentTime = System.currentTimeMillis();

            Long key;
            while((key = blocksClicked.floorKey(currentTime - 1500)) != null) {
                blocksClicked.remove(key);
            }

            long millisDelta = currentTime - lastMillis;
            lastMillis = currentTime;

            durabilityOverrideMap.clear();

            if(pickaxeUseCooldownMillisRemaining >= 0) {
                pickaxeUseCooldownMillisRemaining -= millisDelta;
            }
            if(treecapitatorCooldownMillisRemaining >= 0) {
                treecapitatorCooldownMillisRemaining -= millisDelta;
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        blocksClicked.clear();
    }

    public static long getTreecapCooldownWithPet(){
        if (NotEnoughUpdates.INSTANCE.config.treecap.enableMonkeyCheck && PetInfoOverlay.currentPet != null) {
            PetInfoOverlay.Pet pet = PetInfoOverlay.currentPet;
            if (pet.petLevel != null &&
                pet.petType.equalsIgnoreCase("monkey") &&
                pet.rarity.equals(PetInfoOverlay.Rarity.LEGENDARY)
            ) {
                return 2000 - (int) (2000 * (0.005 * (int) PetInfoOverlay.currentPet.petLevel.level));
            }
        }
        return 2000;
    }

    public static void blockClicked(BlockPos pos) {
        long currentTime = System.currentTimeMillis();
        blocksClicked.put(currentTime, pos);
    }

    public static void processBlockChangePacket(S23PacketBlockChange packetIn) {
        BlockPos pos = packetIn.getBlockPosition();

        if(blocksClicked.containsValue(pos)) {
            IBlockState oldState = Minecraft.getMinecraft().theWorld.getBlockState(pos);
            if(oldState.getBlock() != packetIn.getBlockState().getBlock()) {
                onBlockMined(pos);
            }
        }
    }

    public static void onBlockMined(BlockPos pos) {
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
        if(internalname != null) {
            if(treecapitatorCooldownMillisRemaining < 0 &&
                    (internalname.equals("TREECAPITATOR_AXE") || internalname.equals("JUNGLE_AXE"))) {
                treecapitatorCooldownMillisRemaining = getTreecapCooldownWithPet();
            }
        }
    }

    private static Pattern PICKAXE_ABILITY_REGEX = Pattern.compile("\\u00a7r\\u00a7aYou used your " +
            "\\u00a7r\\u00a7..+ \\u00a7r\\u00a7aPickaxe Ability!\\u00a7r");

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if(PICKAXE_ABILITY_REGEX.matcher(event.message.getFormattedText()).matches()) {
            pickaxeUseCooldownMillisRemaining = 120*1000;
        }
    }

    public static float getDurabilityOverride(ItemStack stack) {
        if(Minecraft.getMinecraft().theWorld == null) return -1;

        if(durabilityOverrideMap.containsKey(stack)) {
            return durabilityOverrideMap.get(stack);
        }

        String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
        if(internalname == null) {
            durabilityOverrideMap.put(stack, -1f);
            return -1;
        }

        if(internalname.endsWith("_PICKAXE") || internalname.contains("_DRILL_")) {
            if(pickaxeUseCooldownMillisRemaining < 0) {
                durabilityOverrideMap.put(stack, -1f);
                return -1;
            }

            if(pickaxeUseCooldownMillisRemaining > 120*1000) {
                return stack.getItemDamage();
            }
            float dura = (float)(pickaxeUseCooldownMillisRemaining/(120.0*1000.0));
            durabilityOverrideMap.put(stack, dura);
            return dura;
        } else if(internalname.equals("TREECAPITATOR_AXE") || internalname.equals("JUNGLE_AXE")) {
            if(treecapitatorCooldownMillisRemaining < 0) {
                durabilityOverrideMap.put(stack, -1f);
                return -1;
            }

            if(treecapitatorCooldownMillisRemaining > getTreecapCooldownWithPet()) {
                return stack.getItemDamage();
            }
            float dura = (treecapitatorCooldownMillisRemaining/(float)getTreecapCooldownWithPet());
            durabilityOverrideMap.put(stack, dura);
            return dura;
        }

        durabilityOverrideMap.put(stack, -1f);
        return -1;
    }

}
