package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.effect.TargetSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 対象選択の実装。
 *
 * 名詞 → 実際の LivingEntity フィルタ。
 * 最初はゾンビ・アンデッド・自分だけ。
 * あとで辞書を増やせばいい。
 */
public final class SpellResolver {

    private static final Random RANDOM = new Random();

    private SpellResolver() {
    }

    public static List<LivingEntity> resolve(
            ServerLevel level,
            ServerPlayer player,
            TargetSelector selector,
            List<String> nouns,
            Double withinBlocks
    ) {
        // self は特別扱い
        if (nouns.contains("self")) {
            return List.of(player);
        }

        // 名前から対象を絞る
        double radius = withinBlocks != null ? withinBlocks : 16.0D;
        AABB area = player.getBoundingBox().inflate(radius);

        List<LivingEntity> candidates = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player) continue;
            if (!e.isAlive()) continue;
            if (matchesNouns(e, nouns)) {
                candidates.add(e);
            }
        }

        // 距離でソート
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        // セレクタで絞る
        return switch (selector) {
            case SELF -> List.of(player);
            case NEAREST -> candidates.isEmpty()
                    ? List.of()
                    : List.of(candidates.get(0));
            case RANDOM -> candidates.isEmpty()
                    ? List.of()
                    : List.of(candidates.get(RANDOM.nextInt(candidates.size())));
            case ALL -> candidates;
        };
    }

    /**
     * 名詞リストに一致するか。
     *
     * 名詞は複数形でも単数形でも許容する。
     * 例: "zombie" でも "zombies" でもゾンビに一致。
     */
    private static boolean matchesNouns(LivingEntity entity, List<String> nouns) {
        if (nouns.isEmpty()) return true;

        for (String noun : nouns) {
            String n = normalize(noun);
            if (matchesNoun(entity, n)) return true;
        }
        return false;
    }

    private static String normalize(String noun) {
        // 複数形 → 単数形への簡易変換
        if (noun.endsWith("ies")) {
            return noun.substring(0, noun.length() - 3) + "y";
        }
        if (noun.endsWith("es")) {
            return noun.substring(0, noun.length() - 2);
        }
        if (noun.endsWith("s")) {
            return noun.substring(0, noun.length() - 1);
        }
        return noun;
    }

    private static boolean matchesNoun(LivingEntity entity, String noun) {
        return switch (noun) {
            case "zombie"   -> entity instanceof Zombie;
            case "undead"   -> entity.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            case "skeleton" -> entity instanceof net.minecraft.world.entity.monster.Skeleton;
            case "creeper"  -> entity instanceof net.minecraft.world.entity.monster.Creeper;
            case "mob"      -> entity instanceof Mob;
            case "entity"   -> true;
            case "player"   -> entity instanceof net.minecraft.world.entity.player.Player;
            default         -> false;
        };
    }
}