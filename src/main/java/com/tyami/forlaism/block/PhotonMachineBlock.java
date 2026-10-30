package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.PhotonMachineBlockEntity;
import com.tyami.forlaism.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class PhotonMachineBlock extends BaseEntityBlock {
    private final PhotonMachineBlockEntity.Kind kind;
    public PhotonMachineBlock(BlockBehaviour.Properties properties, PhotonMachineBlockEntity.Kind kind) {
        super(properties); this.kind = kind;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhotonMachineBlockEntity(pos, state, kind);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PhotonMachineBlockEntity be && player instanceof ServerPlayer sp)
            NetworkHooks.openScreen(sp, be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntities.PHOTON_MACHINE.get(), PhotonMachineBlockEntity::tick);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof PhotonMachineBlockEntity be) be.dropContents();
        super.onRemove(state, level, pos, newState, moving);
    }
}
