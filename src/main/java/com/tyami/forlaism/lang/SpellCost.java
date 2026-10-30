package com.tyami.forlaism.lang;

/**
 * 呪文のコスト。
 *
 * 最初は経験値コストだけ実装。
 * あとでアイテム・マナなどを追加できるようにインターフェース化。
 */
public interface SpellCost {

    /** 支払えるか。 */
    boolean canPay(SpellContext ctx);

    /** 支払う。 */
    void pay(SpellContext ctx);

    /** 経験値コスト。 */
    record Xp(int amount) implements SpellCost {
        @Override
        public boolean canPay(SpellContext ctx) {
            return ctx.player().experienceLevel >= amount;
        }

        @Override
        public void pay(SpellContext ctx) {
            ctx.player().giveExperienceLevels(-amount);
        }
    }

    /** コストなし。 */
    record None() implements SpellCost {
        @Override
        public boolean canPay(SpellContext ctx) {
            return true;
        }

        @Override
        public void pay(SpellContext ctx) {
        }
    }
}