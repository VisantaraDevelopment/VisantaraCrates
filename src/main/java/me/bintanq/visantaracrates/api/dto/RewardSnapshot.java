package me.bintanq.visantaracrates.api.dto;

import me.bintanq.visantaracrates.model.reward.Reward;

/**
 * RewardSnapshot — an immutable, read-only view of a single crate reward.
 *
 * @since 1.4.0
 */
public record RewardSnapshot(
        String id,
        String displayName,
        double weight,
        double chancePercent,
        String rarity,
        String type,
        boolean broadcast
) {
    /**
     * Creates a snapshot from an internal Reward object.
     *
     * @param r           the internal reward
     * @param totalWeight the total weight of all rewards in the crate
     * @return a read-only snapshot
     */
    public static RewardSnapshot from(Reward r, double chancePercent) {
        return new RewardSnapshot(
                r.getId(),
                r.getDisplayName(),
                r.getWeight(),
                chancePercent,
                r.getRarity(),
                r.getType().name(),
                r.isBroadcast()
        );
    }
}
