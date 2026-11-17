package com.questclusters.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClaimLimitsTest {
    private static final UUID TEAM = new UUID(1, 1), A = new UUID(2, 2), B = new UUID(3, 3), OTHER_TEAM = new UUID(4, 4);

    @Test
    void teamAndPlayerLimits() {
        ClaimLimits l = new ClaimLimits();
        l.setReward(10L, new ClaimLimits.RewardLimit(3, 2));
        assertFalse(l.exhausted(10L, TEAM, A));
        l.countClaim(10L, TEAM, A);
        l.countClaim(10L, TEAM, A);
        assertTrue(l.exhausted(10L, TEAM, A), "A used their 2");
        assertFalse(l.exhausted(10L, TEAM, B), "B still may: team has 1 left");
        l.countClaim(10L, TEAM, B);
        assertTrue(l.exhausted(10L, TEAM, B), "team used its 3");
        assertFalse(l.exhausted(10L, OTHER_TEAM, B) && l.playerClaims(B, 10L) < 2, "other team: only B's own count matters");
        assertFalse(l.exhausted(99L, TEAM, A), "no rule, never used up");
    }

    @Test
    void completionLimit() {
        ClaimLimits l = new ClaimLimits();
        assertTrue(l.mayRepeat(5L, 1000), "no rule");
        l.setQuestMax(5L, 3);
        assertTrue(l.mayRepeat(5L, 0));
        assertTrue(l.mayRepeat(5L, 1));
        assertFalse(l.mayRepeat(5L, 2), "third round ends: stays completed");
        l.setQuestMax(5L, 0);
        assertTrue(l.questRules().isEmpty(), "0 removes the rule");
    }

    @Test
    void resetAndRoundTrip() {
        ClaimLimits l = new ClaimLimits();
        l.setQuestMax(5L, 4);
        l.setReward(10L, new ClaimLimits.RewardLimit(1, 0));
        l.setReward(11L, new ClaimLimits.RewardLimit(0, 7));
        l.countClaim(10L, TEAM, A);
        l.countClaim(11L, TEAM, A);
        l.countClaim(11L, TEAM, B);

        ClaimLimits copy = new ClaimLimits();
        copy.readRules(l.writeRules(CompoundTag::new));
        copy.readCounts(l.writeCounts(CompoundTag::new));
        assertEquals(4, copy.questMax(5L));
        assertEquals(new ClaimLimits.RewardLimit(0, 7), copy.reward(11L));
        assertEquals(2, copy.teamClaims(TEAM, 11L));
        assertEquals(1, copy.playerClaims(B, 11L));

        assertEquals(1, copy.resetCounts(null, A, 11L), "one reward of one player");
        assertEquals(0, copy.playerClaims(A, 11L));
        assertEquals(1, copy.playerClaims(A, 10L));
        assertEquals(2, copy.resetCounts(TEAM, null, 0L), "all rewards of the team");
        assertFalse(copy.exhausted(10L, TEAM, A));
    }

    @Test
    void batchRule() {
        ClaimLimits l = new ClaimLimits();
        assertEquals(0, l.batchMax(5L), "off by default");
        l.setBatch(5L, 10);
        l.setBatch(6L, 99_999);
        l.setBatch(7L, -3);
        assertEquals(ClaimLimits.MAX_BATCH, l.batchMax(6L), "capped");
        assertEquals(0, l.batchMax(7L));
        ClaimLimits copy = new ClaimLimits();
        copy.readRules(l.writeRules(CompoundTag::new));
        assertEquals(10, copy.batchMax(5L));
        assertEquals(ClaimLimits.MAX_BATCH, copy.batchMax(6L));
        assertTrue(copy.hasRules());
        copy.setBatch(5L, 0);
        copy.setBatch(6L, 0);
        assertFalse(copy.hasRules(), "0 removes the rule");
    }
}
