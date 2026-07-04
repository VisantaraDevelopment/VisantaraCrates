package me.bintanq.visantaracrates.animation;

import me.bintanq.visantaracrates.model.reward.RewardResult;
import org.bukkit.entity.Player;

public interface CrateAnimation {
    void start(CrateSession session);

    void cancel(CrateSession session);

    boolean isRunning(CrateSession session);
}