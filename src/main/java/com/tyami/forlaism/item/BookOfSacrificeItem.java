package com.tyami.forlaism.item;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 生贄の書。
 *
 * - Mobを右クリックで捕獲（NBT保持）
 * - ブロック右クリックで放出（NO:AI付き）
 * - 1回使い切り
 */
public class BookOfSacrificeItem extends Item implements IMagicEffectItem {

    /** 捕獲したMobのNBTを保存するタグ。 */
    public static final String TAG_CAPTURED_MOB = "CapturedMob";
    public static final String TAG_CAPTURED_TYPE = "CapturedType";

    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80FF2222)
            .intensity(1.6f)
            .scale(1.0f)
            .speed(0.5f)
            .ringCount(0)
            .particleCount(0)
            .flares(true)
            .coreGlow(true)
            .build();

    public BookOfSacrificeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        // 捕獲済みの時だけエフェクト
        return hasCaptured(stack) ? EFFECT_STYLE : null;
    }

    // =========================================================
    // 捕獲: Mobを右クリック
    // =========================================================

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 既に捕獲済みなら何もしない
        if (hasCaptured(stack)) {
            player.displayClientMessage(
                    Component.literal("§c既に何かを捕獲している。"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // Mob以外（プレイヤー等）は捕獲不可
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }

        // ボス等は除外したい場合はここで判定
        if (!mob.isAlive()) {
            return InteractionResult.PASS;
        }

        // ---- NBT保存 ----
        CompoundTag mobTag = new CompoundTag();
        mob.saveWithoutId(mobTag);

        // タイプID
        String typeId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString();

        CompoundTag bookTag = stack.getOrCreateTag();
        bookTag.put(TAG_CAPTURED_MOB, mobTag);
        bookTag.putString(TAG_CAPTURED_TYPE, typeId);

        // 演出
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 0.5F
        );

        player.displayClientMessage(
                Component.literal("§5" + mob.getName().getString() + " §fを捕獲した…"),
                true
        );

        // Mobを消す
        mob.discard();

        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // 放出: ブロック右クリック
    // =========================================================

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 捕獲していないなら何もしない
        if (!hasCaptured(stack)) {
            return InteractionResult.PASS;
        }

        CompoundTag bookTag = stack.getTag();
        if (bookTag == null) {
            return InteractionResult.PASS;
        }

        String typeId = bookTag.getString(TAG_CAPTURED_TYPE);
        CompoundTag mobTag = bookTag.getCompound(TAG_CAPTURED_MOB);

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new net.minecraft.resources.ResourceLocation(typeId)
        );

        if (type == null) {
            player.displayClientMessage(
                    Component.literal("§c捕獲データが壊れている…"),
                    true
            );
            return InteractionResult.FAIL;
        }

        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            player.displayClientMessage(
                    Component.literal("§cMobとして復元できなかった…"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // ---- NBT復元 ----
        mob.load(mobTag);

        // 位置調整
        net.minecraft.core.BlockPos spawnPos = context.getClickedPos()
                .relative(context.getClickedFace());

        mob.moveTo(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                0.0F
        );

        // ---- NO:AI 付与 ----
        mob.setNoAi(true);

        level.addFreshEntity(mob);

        // 演出
        level.playSound(
                null,
                spawnPos,
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS,
                1.0F, 0.8F
        );

        player.displayClientMessage(
                Component.literal("§5" + mob.getName().getString() + " §fを解放した…（§7NoAI§f）"),
                true
        );

        // ---- 使い捨て ----
        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // 捕獲済み判定
    // =========================================================

    public static boolean hasCaptured(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.contains(TAG_CAPTURED_MOB)
                && tag.contains(TAG_CAPTURED_TYPE);
    }

    // =========================================================
    // Tooltip (Shift押しながらで説明表示)
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        // 捕獲済みなら中身を表示
        if (hasCaptured(stack)) {
            CompoundTag tag = stack.getTag();
            String typeId = tag != null ? tag.getString(TAG_CAPTURED_TYPE) : "???";
            tooltip.add(Component.literal("§7捕獲: §f" + typeId).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8ブロックに右クリックで解放").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("§7Mobに右クリックで捕獲").withStyle(ChatFormatting.GRAY));
        }

        // Shift押しながらで詳細説明
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§6【使い方】").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§e・Mobを右クリック §7→ 捕獲").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§e・ブロックを右クリック §7→ 解放 (§cNoAI§7)").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§e・NBTも保持される").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§c※使用すると消滅する（使い捨て）").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("§8[Shift] で使い方を表示").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}