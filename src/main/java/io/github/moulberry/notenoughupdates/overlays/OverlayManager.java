package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.Lists;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay;

import java.util.ArrayList;
import java.util.List;

public class OverlayManager {

    public static Class<? extends TextOverlay> dontRenderOverlay = null;

    public static MiningOverlay miningOverlay;
    public static FarmingOverlay farmingOverlay;
    public static PetInfoOverlay petInfoOverlay;
    public static TimersOverlay timersOverlay;
    public static BonemerangOverlay bonemerangOverlay;
    public static CrystalHollowOverlay crystalHollowOverlay;
    public static final List<TextOverlay> textOverlays = new ArrayList<>();

    static {
        List<String> todoDummy = Lists.newArrayList(
                "\u00a73Cakes: \u00a7eInactive!",
                "\u00a73Cookie Buff: \u00a7eInactive!",
                "\u00a73Godpot: \u00a7eInactive!",
                "\u00a73Puzzler: \u00a7eReady!",
                "\u00a73Fetchur: \u00a7eReady!",
                "\u00a73Commissions: \u00a7eReady!",
                "\u00a73Experiments: \u00a7eReady!",
                "\u00a73Cakes: \u00a7e1d21h",
                "\u00a73Cookie Buff: \u00a7e2d23h",
                "\u00a73Godpot: \u00a7e19h",
                "\u00a73Puzzler: \u00a7e13h",
                "\u00a73Fetchur: \u00a7e3h38m",
                "\u00a73Commissions: \u00a7e3h38m",
                "\u00a73Experiments: \u00a7e3h38m");
        textOverlays.add(timersOverlay = new TimersOverlay(NotEnoughUpdates.INSTANCE.config.miscOverlays.todoPosition, () -> {
            List<String> strings = new ArrayList<>();
            for(int i : NotEnoughUpdates.INSTANCE.config.miscOverlays.todoText2) {
                if(i >= 0 && i < todoDummy.size()) strings.add(todoDummy.get(i));
            }
            return strings;
        }, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.miscOverlays.todoStyle;
            if(style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        }));

        List<String> miningDummy = Lists.newArrayList("\u00a73Goblin Slayer: \u00a7626.5%\n\u00a73Lucky Raffle: \u00a7c0.0%",
                "\u00a73Mithril Powder: \u00a726,243",
                "\u00a73Forge 1) \u00a79Diamonite\u00a77: \u00a7aReady!",
                "\u00a73Forge 2) \u00a77EMPTY\n\u00a73Forge 3) \u00a77EMPTY\n\u00a73Forge 4) \u00a77EMPTY");
        miningOverlay = new MiningOverlay(NotEnoughUpdates.INSTANCE.config.mining.overlayPosition, () -> {
            List<String> strings = new ArrayList<>();
            for(int i : NotEnoughUpdates.INSTANCE.config.mining.dwarvenText2) {
                if(i >= 0 && i < miningDummy.size()) strings.add(miningDummy.get(i));
            }
            return strings;
        }, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.mining.overlayStyle;
            if(style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        });

        List<String> farmingDummy = Lists.newArrayList("\u00a7bCounter: \u00a7e37,547,860",
                "\u00a7bCrops/m: \u00a7e38.29",
                "\u00a7bFarm: \u00a7e12\u00a77 [\u00a7e|||||||||||||||||\u00a78||||||||\u00a77] \u00a7e67%",
                "\u00a7bCurrent XP: \u00a7e6,734",
                "\u00a7bRemaining XP: \u00a7e3,265",
                "\u00a7bXP/h: \u00a7e238,129",
                "\u00a7bYaw: \u00a7e68.25\u00a7l\u1D52");
        farmingOverlay = new FarmingOverlay(NotEnoughUpdates.INSTANCE.config.skillOverlays.farmingPosition, () -> {
            List<String> strings = new ArrayList<>();
            for(int i : NotEnoughUpdates.INSTANCE.config.skillOverlays.farmingText) {
                if(i >= 0 && i < farmingDummy.size()) strings.add(farmingDummy.get(i));
            }
            return strings;
        }, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.skillOverlays.farmingStyle;
            if(style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        });

        List<String> petInfoDummy = Lists.newArrayList("\u00a7a[Lvl 37] \u00a7fRock",
                "\u00a7b2,312.9/2,700\u00a7e (85.7%)",
                "\u00a7b2.3k/2.7k\u00a7e (85.7%)",
                "\u00a7bXP/h: \u00a7e27,209",
                "\u00a7bTotal XP: \u00a7e30,597.9",
                "\u00a7bHeld Item: \u00a7fMining Exp Boost",
                "\u00a7bUntil L38: \u00a7e5m13s",
                "\u00a7bUntil L100: \u00a7e2d13h");
        petInfoOverlay = new PetInfoOverlay(NotEnoughUpdates.INSTANCE.config.petOverlay.petInfoPosition, () -> {
            List<String> strings = new ArrayList<>();
            for(int i : NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayText) {
                if(i >= 0 && i < petInfoDummy.size()) strings.add(petInfoDummy.get(i));
            }
            return strings;
        }, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.petOverlay.petInfoOverlayStyle;
            if(style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        });

        List<String> bonemerangDummy = Lists.newArrayList(
                "\u00a7cBonemerang will break!",
                "\u00a77Targets: \u00a76\u00a7l10"
        );
        bonemerangOverlay = new BonemerangOverlay(NotEnoughUpdates.INSTANCE.config.itemOverlays.bonemerangPosition, () -> bonemerangDummy, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.itemOverlays.bonemerangOverlayStyle;
            if(style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        });
        List<String> crystalHollowOverlayDummy = Lists.newArrayList(
                "\u00a73Amber Crystal: \u00a7aPlaced\n" +
                        "\u00a73Sapphire Crystal: \u00a7eCollected\n" +
                        "\u00a73Jade Crystal: \u00a7eMissing\n" +
                        "\u00a73Amethyst Crystal: \u00a7cMissing\n" +
                        "\u00a73Topaz Crystal: \u00a7cMissing\n",
                "\u00a73Crystals: \u00a7a4/5",
                "\u00a73Crystals: \u00a7a80%",
                "\u00a73Electron Transmitter: \u00a7aDone\n" +
                        "\u00a73Robotron Reflector: \u00a7eIn Storage\n" +
                        "\u00a73Superlite Motor: \u00a7eIn Inventory\n" +
                        "\u00a73Synthetic Hearth: \u00a7cMissing\n" +
                        "\u00a73Control Switch: \u00a7cMissing\n" +
                        "\u00a73FTX 3070: \u00a7cMissing",
                "\u00a73Electron Transmitter: \u00a7a3\n" +
                        "\u00a73Robotron Reflector: \u00a7e2\n" +
                        "\u00a73Superlite Motor: \u00a7e1\n" +
                        "\u00a73Synthetic Hearth: \u00a7c0\n" +
                        "\u00a73Control Switch: \u00a7c0\n" +
                        "\u00a73FTX 3070: \u00a7c0",
                "\u00a73Automaton parts: \u00a7a5/6",
                "\u00a73Automaton parts: \u00a7a83%",
                "\u00a73Scavenged Lapis Sword: \u00a7aDone\n" +
                        "\u00a73Scavenged Golden Hammer: \u00a7eIn Storage\n" +
                        "\u00a73Scavenged Diamond Axe: \u00a7eIn Inventory\n" +
                        "\u00a73Scavenged Emerald Hammer: \u00a7cMissing\n",
                "\u00a73Scavenged Lapis Sword: \u00a7a3\n" +
                        "\u00a73Scavenged Golden Hammer: \u00a7e2\n" +
                        "\u00a73Scavenged Diamond Axe: \u00a7e1\n" +
                        "\u00a73Scavenged Emerald Hammer: \u00a7c0\n",
                "\u00a73Mines of Divan parts: \u00a7a3/4",
                "\u00a73Mines of Divan parts: \u00a7a75%"
        );
        crystalHollowOverlay = new CrystalHollowOverlay(NotEnoughUpdates.INSTANCE.config.mining.crystalHollowOverlayPosition, () -> {
            List<String> strings = new ArrayList<>();
            for (int i : NotEnoughUpdates.INSTANCE.config.mining.crystalHollowText) {
                if (i >= 0 && i < crystalHollowOverlayDummy.size()) strings.add(crystalHollowOverlayDummy.get(i));
            }
            return strings;
        }, () -> {
            int style = NotEnoughUpdates.INSTANCE.config.mining.crystalHollowOverlayStyle;
            if (style >= 0 && style < TextOverlayStyle.values().length) {
                return TextOverlayStyle.values()[style];
            }
            return TextOverlayStyle.BACKGROUND;
        });

        textOverlays.add(miningOverlay);
        textOverlays.add(farmingOverlay);
        textOverlays.add(petInfoOverlay);
        textOverlays.add(bonemerangOverlay);
        textOverlays.add(crystalHollowOverlay);
    }

}
