package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * 対象選択。
 *
 * 例:
 *   the nearest zombie
 *     → qualifier=NEAREST, nouns=["zombie"]
 *   all zombies
 *     → qualifier=ALL, nouns=["zombie"]
 *   zombies within 5 blocks
 *     → qualifier=ALL, nouns=["zombie"], within=5
 *   a random zombie
 *     → qualifier=RANDOM, nouns=["zombie"]
 *
 * qualifier は文字列で持つ（"the"/"all"/"a"/null）。
 * nouns は複数形/単数形をそのまま入れる（辞書側で正規化）。
 */
public record TargetNode(
        String qualifier,
        List<String> nouns,
        Double withinBlocks
) implements Node {
}