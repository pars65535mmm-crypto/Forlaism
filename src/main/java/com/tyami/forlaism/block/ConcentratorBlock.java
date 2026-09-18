package com.tyami.forlaism.block;
import com.tyami.forlaism.block.entity.ConcentratorBlockEntity;
import com.tyami.forlaism.block.entity.Tier2MachineBlockEntity;
import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Blocks;
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
public class ConcentratorBlock extends BaseEntityBlock {
    public ConcentratorBlock(BlockBehaviour.Properties p){super(p);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s){return new ConcentratorBlockEntity(p,s);}
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof ConcentratorBlockEntity be&&pl instanceof ServerPlayer sp)NetworkHooks.openScreen(sp,be,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t){return createTickerHelper(t, BlockEntities.FORLAISM_CONCENTRATOR.get(), Tier2MachineBlockEntity::tick);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState ns,boolean m){if(s.getBlock()!=ns.getBlock()&&l.getBlockEntity(p) instanceof ConcentratorBlockEntity be)be.drops();super.onRemove(s,l,p,ns,m);}
}
