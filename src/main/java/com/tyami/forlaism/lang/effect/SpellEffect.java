package com.tyami.forlaism.lang.effect;

public enum SpellEffect {
    // 既存
    IGNITE, FREEZE, HEAL, ERASE, TELEPORT, SUMMON,

    // 取得系
    GET_POS, GET_HEALTH, GET_MAX_HEALTH, GET_LOOK, GET_MOTION,
    GET_UUID_HASH, GET_X, GET_Y, GET_Z,

    // ベクトル系
    VEC_ADD, VEC_SUB, VEC_MUL, VEC_NORMALIZE, VEC_LENGTH,

    // 位置操作
    SET_POS, SET_MOTION, ADD_MOTION, TP_LOOK,

    // レイキャスト
    RAYCAST,

    // 演出系
    LIGHTNING, EXPLODE, PARTICLE, SOUND,

    // 状態系
    CLEAR_EFFECTS, GIVE_EFFECT, DAMAGE, KILL
}