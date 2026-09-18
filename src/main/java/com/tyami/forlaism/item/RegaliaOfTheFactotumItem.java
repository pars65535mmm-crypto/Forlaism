package com.tyami.forlaism.item;

import com.tyami.forlaism.damage.FactotumDamage;
import com.tyami.forlaism.entity.FactotumMinionEntity;
import com.tyami.forlaism.entity.FactotumPhantomEntity;
import com.tyami.forlaism.util.FactotumMiningHelper;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class RegaliaOfTheFactotumItem extends Item implements IAnimatedTextItem {

    public enum Mode {
        MACE("Mace / 灯笏", 0.0F),
        PAXEL("Paxel / パクセル", 0.5F),
        DAGGER("Dagger / ダガー", 1.0F);

        private final String displayName;
        private final float propertyValue;

        Mode(String displayName, float propertyValue) {
            this.displayName = displayName;
            this.propertyValue = propertyValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float getPropertyValue() {
            return propertyValue;
        }

        public Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private static final String TAG_MODE = "factotum_mode";
    private static final String TAG_MINING_RANGE = "factotum_mining_range";
    public static final int PHANTOM_COOLDOWN_TICKS = 300; // 15秒

    public RegaliaOfTheFactotumItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    public Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            int ord = tag.getInt(TAG_MODE);
            if (ord >= 0 && ord < Mode.values().length) {
                return Mode.values()[ord];
            }
        }
        return Mode.MACE;
    }

    public void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    public int getMiningRange(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MINING_RANGE)) {
            return Math.max(1, Math.min(16, tag.getInt(TAG_MINING_RANGE)));
        }
        return 3;
    }

    public void setMiningRange(ItemStack stack, int range) {
        stack.getOrCreateTag().putInt(TAG_MINING_RANGE, Math.max(1, Math.min(16, range)));
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        // 赤・オレンジ・白・水色の4色グラデーション
        String text = DistExecutor.unsafeRunForDist(
                () -> () -> {
                    String lang = Minecraft.getInstance().getLanguageManager().getSelected();
                    return lang != null && lang.startsWith("ja") ? "不可なる彼々の灯籠" : "Regalia of the Factotum";
                },
                () -> () -> "Regalia of the Factotum"
        );

        return AnimatedText.of(text)
                .wave(2.5f, 0.15f, 0.5f)
                .gradient(0xFFFF0000, 0xFFFF7700, 0xFFFFFFFF, 0xFF00FFFF)
                .gradientSpeed(0.08f)
                .gradientPhase(0.5f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // SHIFT + 右クリック: 各形態の特殊能力
            Mode mode = getMode(stack);
            if (mode == Mode.MACE) {
                handleMaceSpecial(level, player);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            } else if (mode == Mode.DAGGER) {
                handleDaggerSpecial(level, player);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        } else {
            // 通常右クリック: 形態変更
            if (!level.isClientSide) {
                Mode prevMode = getMode(stack);
                Mode nextMode = prevMode.next();
                setMode(stack, nextMode);

                player.displayClientMessage(
                        Component.literal("§6[Forlaism] §eMode switched: §f" + nextMode.getDisplayName()),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.8F, 1.2F);

                // パクセル → ダガー切り替え時: 幻影を10体生成
                if (prevMode == Mode.PAXEL && nextMode == Mode.DAGGER) {
                    spawnPhantoms(level, player, 10);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return super.use(level, player, hand);
    }

    /**
     * メイス形態特殊操作: 眷属召喚 / 一斉突撃
     */
    private void handleMaceSpecial(Level level, Player player) {
        if (level.isClientSide) return;

        List<FactotumMinionEntity> existingMinions = level.getEntitiesOfClass(FactotumMinionEntity.class,
                player.getBoundingBox().inflate(64.0),
                m -> player.getUUID().equals(m.getOwnerUUID()) && m.getMinionState() == FactotumMinionEntity.State.WAITING);

        if (existingMinions.isEmpty()) {
            // 待機中の眷属がいない場合は7体召喚
            int count = 7;
            for (int i = 0; i < count; i++) {
                FactotumMinionEntity minion = new FactotumMinionEntity(level, player, i, count);
                level.addFreshEntity(minion);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.displayClientMessage(Component.literal("§6[Forlaism] §eSummoned Factotum Minions."), true);
        } else {
            // すでに展開されている場合は敵へ突撃命令
            LivingEntity target = findLookTarget(player, 120.0D);
            int delay = 0;
            for (FactotumMinionEntity minion : existingMinions) {
                minion.commandAttack(target, delay);
                delay += 3; // 順番に殺到
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.2F, 1.4F);
            player.displayClientMessage(
                    Component.literal(target != null ? "§6[Forlaism] §cMinions assaulting target!" : "§6[Forlaism] §eMinions rushed forward!"),
                    true
            );
        }
    }

    /**
     * ダガー形態特殊操作: 幻影7体生成
     */
    private void handleDaggerSpecial(Level level, Player player) {
        if (level.isClientSide) return;

        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.literal("§cPhantom skill is on cooldown!"), true);
            return;
        }

        spawnPhantoms(level, player, 7);
    }

    /**
     * 幻影Entity生成処理（15秒クールタイム付き）
     */
    private void spawnPhantoms(Level level, Player player, int count) {
        if (level.isClientSide) return;

        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }

        for (int i = 0; i < count; i++) {
            FactotumPhantomEntity phantom = new FactotumPhantomEntity(level, player);
            level.addFreshEntity(phantom);
        }

        player.getCooldowns().addCooldown(this, PHANTOM_COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("§6[Forlaism] §dSummoned " + count + " Phantoms!"), true);
    }

    /**
     * プレイヤーの視線前方にある敵を取得
     */
    public static LivingEntity findLookTarget(Player player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDist));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(2.0);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                end,
                searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
        if (getMode(itemstack) == Mode.PAXEL) {
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                int range = getMiningRange(itemstack);
                FactotumMiningHelper.mineArea(serverPlayer.serverLevel(), serverPlayer, pos, range, itemstack);
            }
            return true;
        }
        return super.onBlockStartBreak(itemstack, pos, player);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        if (getMode(player.getMainHandItem()) == Mode.PAXEL) {
            return true;
        }
        return super.canAttackBlock(state, level, pos, player);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (getMode(stack) == Mode.PAXEL) {
            return 9999.0F; // 瞬時に破壊可能
        }
        return super.getDestroySpeed(stack, state);
    }
}
