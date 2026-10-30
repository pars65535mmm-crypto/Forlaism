package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.ast.*;
import com.tyami.forlaism.lang.effect.Element;
import com.tyami.forlaism.lang.effect.SpellEffect;
import com.tyami.forlaism.lang.effect.TargetSelector;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 実行エンジン（B構文対応）。
 *
 * 関数呼び出し形式と従来動詞形式を両方受け付ける。
 */
public final class Interpreter {

    private final SpellDictionary dictionary;
    private final Environment spellEnv;
    private final Map<String, Environment> scopeEnvs;
    private final Map<String, FunctionDefNode> functions;

    public Interpreter(
            SpellDictionary dictionary,
            Environment playerEnv,
            Environment itemEnv,
            Environment worldEnv
    ) {
        this(dictionary, playerEnv, itemEnv, worldEnv, new HashMap<>());
    }

    public Interpreter(
            SpellDictionary dictionary,
            Environment playerEnv,
            Environment itemEnv,
            Environment worldEnv,
            Map<String, FunctionDefNode> inheritedFunctions
    ) {
        this.dictionary = dictionary;
        this.spellEnv = new Environment(null);
        this.scopeEnvs = new HashMap<>();
        scopeEnvs.put("private", spellEnv);
        scopeEnvs.put("player", new Environment(playerEnv));
        scopeEnvs.put("item", new Environment(itemEnv));
        scopeEnvs.put("world", new Environment(worldEnv));
        this.functions = inheritedFunctions;
    }

    public void execute(ProgramNode program, SpellContext ctx) {
        for (Node stmt : program.statements()) executeStatement(stmt, ctx);
    }

    public void executeStatement(Node node, SpellContext ctx) {
        if (node instanceof AssignNode assign) {
            Object val = evalValue(assign.value(), ctx);
            Environment env = scopeEnvs.get(assign.scope());
            if (val instanceof Vec3 v) env.setVec(assign.name(), v);
            else if (val instanceof Double d) env.set(assign.name(), d);
            else throw new LangException("forlaism.lang.error.not_expression");
            return;
        }

        if (node instanceof IfNode iff) {
            double cond = evalNumber(iff.condition(), ctx);
            if (cond != 0) executeStatement(iff.body(), ctx);
            else if (iff.hasElse()) executeStatement(iff.elseBody(), ctx);
            return;
        }

        if (node instanceof FunctionCallExprNode call) {
            executeFunctionCall(call, ctx);
            return;
        }

        if (node instanceof CommandNode cmd) {
            executeCommand(cmd, ctx);
            return;
        }

        if (node instanceof FunctionDefNode def) {
            functions.put(def.name().toLowerCase(), def);
            return;
        }

        if (node instanceof PrintNode print) {
            executePrint(print, ctx);
            return;
        }

        if (node instanceof LoopNode) {
            throw new LangException("forlaism.lang.error.loop_not_scheduled");
        }

        throw new LangException("forlaism.lang.error.unknown_node",
                node.getClass().getSimpleName());
    }

    // =========================================================
    // 式の評価
    // =========================================================

    private Object evalValue(Node node, SpellContext ctx) {
        if (node instanceof VecLiteralNode v) {
            return new Vec3(
                    evalNumber(v.x(), ctx),
                    evalNumber(v.y(), ctx),
                    evalNumber(v.z(), ctx)
            );
        }

        if (node instanceof FunctionCallExprNode call) {
            return evalFunctionCall(call, ctx);
        }

        // ===========================================
        // 名詞句(TargetNode) → 最近傍の LivingEntity を返す
        // ===========================================
        if (node instanceof TargetNode tn) {
            List<LivingEntity> targets = ctx.resolveTargets(
                    resolveSelector(tn),
                    tn.nouns(),
                    tn.withinBlocks() == null ? 16.0 : tn.withinBlocks()
            );
            if (targets.isEmpty()) return null;

            // 最近傍を返す
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity e : targets) {
                double d = e.distanceToSqr(ctx.player());
                if (d < bestDist) { bestDist = d; best = e; }
            }
            return best;
        }

        return evalNumber(node, ctx);
    }

    private double evalNumber(Node node, SpellContext ctx) {
        if (node instanceof NumberNode n) return n.value();
        if (node instanceof StringNode s) return s.value().hashCode();

        if (node instanceof VariableNode v) {
            for (String scope : new String[]{"private", "player", "item", "world"}) {
                Environment env = scopeEnvs.get(scope);
                if (env != null && env.hasOwn(v.name())) {
                    if (env.isVector(v.name())) return env.getVec(v.name()).x;
                    return env.get(v.name());
                }
            }
            throw new LangException("forlaism.lang.error.unknown_variable", v.name());
        }

        if (node instanceof VecLiteralNode v) return evalNumber(v.x(), ctx);

        if (node instanceof BinaryOpNode b) {
            double l = evalNumber(b.left(), ctx);
            double r = evalNumber(b.right(), ctx);
            return switch (b.op()) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "/" -> r == 0 ? 0 : l / r;
                case ">" -> l > r ? 1 : 0;
                case "<" -> l < r ? 1 : 0;
                case ">=" -> l >= r ? 1 : 0;
                case "<=" -> l <= r ? 1 : 0;
                case "=", "==" -> l == r ? 1 : 0;
                case "!=" -> l != r ? 1 : 0;
                default -> throw new LangException("forlaism.lang.error.unknown_op", b.op());
            };
        }

        if (node instanceof FunctionCallExprNode call) {
            Object v = evalFunctionCall(call, ctx);
            if (v instanceof Double d) return d;
            if (v instanceof Vec3 vec) return vec.x;
        }

        throw new LangException("forlaism.lang.error.not_expression");
    }

    // =========================================================
    // 関数呼び出しの評価
    // =========================================================

    private Object evalFunctionCall(FunctionCallExprNode call, SpellContext ctx) {
        String name = call.name().toLowerCase();
        List<Node> args = call.args();

        switch (name) {
            // ===== 取得系 =====
            case "getpos" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? Vec3.ZERO : t.position();
            }
            case "getlook" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? Vec3.ZERO : t.getLookAngle();
            }
            case "getmotion" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? Vec3.ZERO : t.getDeltaMovement();
            }
            case "gethealth" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : (double) t.getHealth();
            }
            case "getmaxhealth" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : (double) t.getMaxHealth();
            }
            case "getuuidhash" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : (double) t.getUUID().hashCode();
            }
            case "getx" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : t.getX();
            }
            case "gety" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : t.getY();
            }
            case "getz" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                return t == null ? 0.0 : t.getZ();
            }

            // ===== ベクトル系 =====
            case "vecof" -> {
                double x = requireNum(args, 0, ctx);
                double y = requireNum(args, 1, ctx);
                double z = requireNum(args, 2, ctx);
                return new Vec3(x, y, z);
            }
            case "vecadd" -> {
                Vec3 a = requireVec(args, 0, ctx);
                Vec3 b = requireVec(args, 1, ctx);
                return a.add(b);
            }
            case "vecsub" -> {
                Vec3 a = requireVec(args, 0, ctx);
                Vec3 b = requireVec(args, 1, ctx);
                return a.subtract(b);
            }
            case "vecmul" -> {
                Vec3 a = requireVec(args, 0, ctx);
                double s = requireNum(args, 1, ctx);
                return a.scale(s);
            }
            case "vecnormalize" -> {
                Vec3 a = requireVec(args, 0, ctx);
                return a.normalize();
            }
            case "veclength" -> {
                Vec3 a = requireVec(args, 0, ctx);
                return a.length();
            }

            // ===== レイキャスト =====
            case "raycast" -> {
                LivingEntity caster = resolveArgAsEntity(args, 0, ctx);
                double dist = requireNum(args, 1, ctx);
                if (caster == null) return Vec3.ZERO;
                return doRaycast(caster, dist, ctx);
            }

            // ===== 位置・速度操作 =====
            case "setmotion" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                Vec3 v = requireVec(args, 1, ctx);
                if (t != null) {
                    t.setDeltaMovement(v);
                    t.hurtMarked = true;
                }
                return 0.0;
            }
            case "addmotion" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                Vec3 v = requireVec(args, 1, ctx);
                if (t != null) {
                    t.setDeltaMovement(t.getDeltaMovement().add(v));
                    t.hurtMarked = true;
                }
                return 0.0;
            }
            case "setpos" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                Vec3 v = requireVec(args, 1, ctx);
                if (t != null) t.teleportTo(v.x, v.y, v.z);
                return 0.0;
            }

            // ===== 演出系 =====
            case "particle" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                String element = requireString(args, 1, ctx);
                int count = (int) requireNum(args, 2, ctx);
                if (t != null) doParticle(ctx, t, element, count);
                return 0.0;
            }
            case "sound" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                String element = requireString(args, 1, ctx);
                if (t != null) doSound(ctx, t, element);
                return 0.0;
            }

            // ===== 状態系 =====
            case "damage" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                float amount = (float) requireNum(args, 1, ctx);
                if (t != null) {
                    t.invulnerableTime = 0;
                    t.hurt(ctx.level().damageSources().indirectMagic(ctx.player(), ctx.player()), amount);
                }
                return 0.0;
            }
            case "kill" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                if (t != null) {
                    t.setHealth(0.0F);
                    t.die(ctx.level().damageSources().indirectMagic(ctx.player(), ctx.player()));
                }
                return 0.0;
            }
            case "heal" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                float amount = (float) requireNum(args, 1, ctx);
                if (t != null) t.heal(amount);
                return 0.0;
            }
            case "ignite" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                int seconds = (int) requireNum(args, 1, ctx);
                float dmg = (float) requireNum(args, 2, ctx);
                if (t != null) {
                    t.setSecondsOnFire(seconds);
                    t.hurt(ctx.level().damageSources().indirectMagic(ctx.player(), ctx.player()), dmg);
                }
                return 0.0;
            }
            case "freeze" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                int ticks = (int) requireNum(args, 1, ctx);
                int amp = (int) requireNum(args, 2, ctx);
                if (t != null) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
                return 0.0;
            }
            case "lightning" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                if (t != null) {
                    var bolt = EntityType.LIGHTNING_BOLT.create(ctx.level());
                    if (bolt != null) {
                        bolt.moveTo(t.getX(), t.getY(), t.getZ());
                        bolt.setVisualOnly(false);
                        ctx.level().addFreshEntity(bolt);
                    }
                }
                return 0.0;
            }
            case "explode" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                float power = (float) requireNum(args, 1, ctx);
                if (t != null) {
                    ctx.level().explode(null, t.getX(), t.getY(), t.getZ(),
                            power, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                }
                return 0.0;
            }
            case "teleport" -> {
                LivingEntity t = resolveArgAsEntity(args, 0, ctx);
                double dist = requireNum(args, 1, ctx);
                if (t != null) {
                    Vec3 look = ctx.player().getLookAngle().normalize();
                    Vec3 dest = t.position().add(look.scale(dist));
                    t.teleportTo(dest.x, dest.y, dest.z);
                }
                return 0.0;
            }
        }

        throw new LangException("forlaism.lang.error.unknown_function", call.name());
    }

    private void executeFunctionCall(FunctionCallExprNode call, SpellContext ctx) {
        // 戻り値は無視（文として呼ばれた）
        evalFunctionCall(call, ctx);
    }

    // =========================================================
    // 引数解決ヘルパー
    // =========================================================

    /**
     * 引数を LivingEntity として解決。
     *  VariableNode ("self", "zombie" 等) → 名詞として検索
     *  TargetNode (複数単語) → その名詞句として検索
     */
    private LivingEntity resolveArgAsEntity(List<Node> args, int index, SpellContext ctx) {
        if (index >= args.size()) return ctx.player();
        Node arg = args.get(index);

        // 単語1個
        if (arg instanceof VariableNode v) {
            String noun = v.name().toLowerCase();
            if (noun.equals("self")) return ctx.player();
            List<LivingEntity> targets = ctx.resolveTargets(
                    TargetSelector.ALL, List.of(noun), 16.0
            );
            return nearest(ctx.player(), targets);
        }

        // 名詞句
        if (arg instanceof TargetNode tn) {
            TargetSelector sel = resolveSelector(tn);
            List<LivingEntity> targets = ctx.resolveTargets(
                    sel, tn.nouns(),
                    tn.withinBlocks() == null ? 16.0 : tn.withinBlocks()
            );
            return targets.isEmpty() ? null : targets.get(0);
        }

        return ctx.player();
    }

    private LivingEntity nearest(LivingEntity player, List<LivingEntity> targets) {
        if (targets.isEmpty()) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : targets) {
            double d = e.distanceToSqr(player);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return best;
    }

    private Vec3 requireVec(List<Node> args, int index, SpellContext ctx) {
        if (index >= args.size()) return Vec3.ZERO;
        return evalAsVec(args.get(index), ctx);
    }

    /**
     * ノードを Vec3 として評価する。
     *  VariableNode が Vec3 ならそれを返す。
     *  FunctionCallExprNode が Vec3 を返すならそれを返す。
     *  数値なら X だけのベクトル。
     */
    private Vec3 evalAsVec(Node node, SpellContext ctx) {
        // 変数がベクターなら直接取得
        if (node instanceof VariableNode v) {
            for (String scope : new String[]{"private", "player", "item", "world"}) {
                Environment env = scopeEnvs.get(scope);
                if (env != null && env.isVector(v.name())) {
                    return env.getVec(v.name());
                }
            }
        }

        Object v = evalValue(node, ctx);
        if (v instanceof Vec3 vec) return vec;
        if (v instanceof Double d) return new Vec3(d, 0, 0);
        return Vec3.ZERO;
    }

    private double requireNum(List<Node> args, int index, SpellContext ctx) {
        if (index >= args.size()) return 0.0;
        Object v = evalValue(args.get(index), ctx);
        if (v instanceof Double d) return d;
        if (v instanceof Vec3 vec) return vec.x;
        return 0.0;
    }

    private String requireString(List<Node> args, int index, SpellContext ctx) {
        if (index >= args.size()) return "light";
        Node n = args.get(index);
        if (n instanceof StringNode s) return s.value();
        if (n instanceof VariableNode v) return v.name();
        return "light";
    }

    // =========================================================
    // 従来の動詞形式（後方互換）
    // =========================================================

    private void executeCommand(CommandNode cmd, SpellContext ctx) {
        SpellEffect effect = dictionary.effectOf(cmd.verb());
        Element element = dictionary.elementOf(cmd.element());

        List<LivingEntity> targets;
        if (cmd.target() == null) targets = List.of(ctx.player());
        else targets = ctx.resolveTargets(
                resolveSelector(cmd.target()),
                cmd.target().nouns(),
                cmd.target().withinBlocks()
        );

        if (targets.isEmpty()) return;

        for (LivingEntity target : targets) {
            applyLegacyEffect(ctx, effect, element, target, cmd);
        }

        if (element != Element.NONE) spawnCastParticles(ctx, element);
    }

    private TargetSelector resolveSelector(TargetNode target) {
        if (target.nouns().contains("self")) return TargetSelector.SELF;
        String q = target.qualifier();
        if (q == null) return TargetSelector.ALL;
        return switch (q) {
            case "the", "an" -> TargetSelector.NEAREST;
            case "a" -> TargetSelector.RANDOM;
            case "all" -> TargetSelector.ALL;
            default -> TargetSelector.ALL;
        };
    }

    private void applyLegacyEffect(
            SpellContext ctx,
            SpellEffect effect,
            Element element,
            LivingEntity target,
            CommandNode cmd
    ) {
        ServerLevel level = ctx.level();

        switch (effect) {
            case IGNITE -> {
                int seconds = (int) argNum(cmd, 0, 8, ctx);
                float dmg = (float) argNum(cmd, 1, 5.0, ctx);
                target.setSecondsOnFire(seconds);
                target.hurt(level.damageSources().indirectMagic(ctx.player(), ctx.player()), dmg);
            }
            case FREEZE -> {
                int ticks = (int) argNum(cmd, 0, 100, ctx);
                int amp = (int) argNum(cmd, 1, 4, ctx);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
            }
            case HEAL -> {
                float amount = (float) argNum(cmd, 0, 4.0, ctx);
                int amp = (int) argNum(cmd, 1, 1, ctx);
                target.heal(amount);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, amp));
            }
            case ERASE, KILL -> {
                target.setHealth(0.0F);
                target.die(level.damageSources().indirectMagic(ctx.player(), ctx.player()));
            }
            case TELEPORT -> {
                double dist = argNum(cmd, 0, 5.0, ctx);
                Vec3 look = ctx.player().getLookAngle().normalize();
                Vec3 dest = target.position().add(look.scale(dist));
                target.teleportTo(dest.x, dest.y, dest.z);
            }
            case TP_LOOK -> {
                double dist = argNum(cmd, 0, 10.0, ctx);
                Vec3 eye = ctx.player().getEyePosition();
                Vec3 look = ctx.player().getLookAngle().normalize();
                Vec3 dest = eye.add(look.scale(dist));
                ctx.player().teleportTo(dest.x, dest.y, dest.z);
            }
            case SET_POS -> {
                Vec3 v = requireVec(cmd.args(), 0, ctx);
                target.teleportTo(v.x, v.y, v.z);
            }
            case SET_MOTION -> {
                Vec3 v = requireVec(cmd.args(), 0, ctx);
                target.setDeltaMovement(v);
                target.hurtMarked = true;
            }
            case ADD_MOTION -> {
                Vec3 v = requireVec(cmd.args(), 0, ctx);
                target.setDeltaMovement(target.getDeltaMovement().add(v));
                target.hurtMarked = true;
            }
            case DAMAGE -> {
                float amount = (float) argNum(cmd, 0, 1.0, ctx);
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().indirectMagic(ctx.player(), ctx.player()), amount);
            }
            case CLEAR_EFFECTS -> target.removeAllEffects();
            case LIGHTNING -> {
                var bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(false);
                    level.addFreshEntity(bolt);
                }
            }
            case EXPLODE -> {
                float power = (float) argNum(cmd, 0, 2.0F, ctx);
                level.explode(null, target.getX(), target.getY(), target.getZ(),
                        power, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
            default -> {}
        }

        if (element == Element.FIRE) target.setSecondsOnFire(4);
        if (element == Element.ICE) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        if (element == Element.LIGHT) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        if (element == Element.DARK) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
    }

    private double argNum(CommandNode cmd, int index, double def, SpellContext ctx) {
        if (index >= cmd.args().size()) return def;
        Object v = evalValue(cmd.args().get(index), ctx);
        if (v instanceof Double d) return d;
        if (v instanceof Vec3 vec) return vec.x;
        return def;
    }

    // =========================================================
    // レイキャスト内部実装
    // =========================================================

    private Vec3 doRaycast(LivingEntity caster, double maxDist, SpellContext ctx) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(maxDist));
        ServerLevel level = ctx.level();

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;

        AABB box = caster.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, caster, eye, end, box,
                e -> e instanceof LivingEntity && e != caster
        );
        double entityDist = entityHit != null
                ? eye.distanceTo(entityHit.getLocation()) : Double.MAX_VALUE;

        if (entityDist < blockDist && entityHit != null) return entityHit.getLocation();
        if (blockHit.getType() == HitResult.Type.BLOCK) return blockHit.getLocation();
        return end;
    }

    // =========================================================
    // パーティクル / サウンド
    // =========================================================

    private void doParticle(SpellContext ctx, LivingEntity t, String element, int count) {
        var particle = switch (element.toLowerCase()) {
            case "fire" -> ParticleTypes.FLAME;
            case "ice" -> ParticleTypes.SNOWFLAKE;
            case "light" -> ParticleTypes.END_ROD;
            case "dark" -> ParticleTypes.SQUID_INK;
            case "moonlight" -> ParticleTypes.END_ROD;
            case "void" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
        ctx.level().sendParticles(particle,
                t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(),
                count, 0.4, 0.4, 0.4, 0.05);
    }

    private void doSound(SpellContext ctx, LivingEntity t, String element) {
        var sound = switch (element.toLowerCase()) {
            case "fire" -> SoundEvents.BLAZE_SHOOT;
            case "ice" -> SoundEvents.GLASS_BREAK;
            case "light" -> SoundEvents.BEACON_ACTIVATE;
            case "dark" -> SoundEvents.WARDEN_ROAR;
            default -> SoundEvents.ENCHANTMENT_TABLE_USE;
        };
        ctx.level().playSound(null, t.getX(), t.getY(), t.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private void spawnCastParticles(SpellContext ctx, Element element) {
        ServerLevel level = ctx.level();
        Vec3 pos = ctx.player().getEyePosition().add(ctx.player().getLookAngle().scale(1.0));
        var particle = switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHT -> ParticleTypes.END_ROD;
            case DARK -> ParticleTypes.SQUID_INK;
            case MOONLIGHT -> ParticleTypes.END_ROD;
            case VOID -> ParticleTypes.PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
        level.sendParticles(particle, pos.x, pos.y, pos.z, 15, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, ctx.player().blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private void executePrint(PrintNode print, SpellContext ctx) {
        String text;
        Node body = print.body();

        if (body instanceof StringNode s) {
            text = s.value();
        } else {
            Object v = evalValue(body, ctx);
            if (v instanceof Vec3 vec) {
                text = String.format("(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
            } else if (v instanceof Double d) {
                text = (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) (double) d)
                        : String.valueOf(d);
            } else {
                text = String.valueOf(v);
            }
        }
        ctx.player().displayClientMessage(Component.literal(text), false);
    }

    public Map<String, FunctionDefNode> getFunctions() {
        return functions;
    }
}