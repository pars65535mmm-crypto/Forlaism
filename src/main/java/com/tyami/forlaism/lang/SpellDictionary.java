package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.effect.Element;
import com.tyami.forlaism.lang.effect.SpellEffect;

import java.util.HashMap;
import java.util.Map;

/**
 * 呪文辞書。
 *
 * 動詞 → 効果
 * 属性 → Element
 * 名詞 → 対象（SpellResolver 側で処理）
 *
 * ここに登録されていない動詞はエラー。
 *
 * 将来的には JSON から読み込めるようにする。
 */
public final class SpellDictionary {

    private final Map<String, SpellEffect> effects = new HashMap<>();
    private final Map<String, SpellCost> costs = new HashMap<>();
    private final Map<String, Element> elements = new HashMap<>();

    public SpellDictionary() {
        // 動詞
        effects.put("ignite", SpellEffect.IGNITE);
        effects.put("freeze", SpellEffect.FREEZE);
        effects.put("heal",   SpellEffect.HEAL);
        effects.put("erase",  SpellEffect.ERASE);
        effects.put("summon", SpellEffect.SUMMON);
        effects.put("teleport", SpellEffect.TELEPORT);

        // デフォルトコスト（動詞ごと）
        costs.put("ignite", new SpellCost.Xp(5));
        costs.put("freeze", new SpellCost.Xp(5));
        costs.put("heal",   new SpellCost.Xp(3));
        costs.put("erase",  new SpellCost.Xp(50));
        costs.put("summon", new SpellCost.Xp(20));
        costs.put("teleport", new SpellCost.Xp(10));

        // 属性
        elements.put("fire",      Element.FIRE);
        elements.put("ice",       Element.ICE);
        elements.put("light",     Element.LIGHT);
        elements.put("dark",      Element.DARK);
        elements.put("moonlight", Element.MOONLIGHT);
        elements.put("void",      Element.VOID);
    }

    public SpellEffect effectOf(String verb) {
        SpellEffect e = effects.get(verb.toLowerCase());
        if (e == null) {
            throw new LangException("forlaism.lang.error.unknown_verb", verb);
        }
        return e;
    }

    public SpellCost costOf(String verb) {
        return costs.getOrDefault(verb.toLowerCase(), new SpellCost.None());
    }

    public Element elementOf(String name) {
        if (name == null) return Element.NONE;
        return elements.getOrDefault(name.toLowerCase(), Element.NONE);
    }
}