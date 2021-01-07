package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.StreamerMode;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({GuiIngame.class})
public class MixinGuiIngame {

    private static final String TARGET = "Lnet/minecraft/scoreboard/ScorePlayerTeam;" +
            "formatPlayerName(Lnet/minecraft/scoreboard/Team;Ljava/lang/String;)Ljava/lang/String;";
    @Redirect(method="renderScoreboard", at=@At(value="INVOKE", target=TARGET))
    public String renderScoreboard_formatPlayerName(Team team, String name) {
        if(NotEnoughUpdates.INSTANCE.isOnSkyblock() && NotEnoughUpdates.INSTANCE.config.misc.streamerMode) {
            return StreamerMode.filterScoreboard(ScorePlayerTeam.formatPlayerName(team, name));
        }
        return ScorePlayerTeam.formatPlayerName(team, name);
    }
}
