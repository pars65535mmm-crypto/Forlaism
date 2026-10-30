package com.tyami.forlaism.jade;

import com.tyami.forlaism.block.entity.PhotonMachineBlockEntity;
import com.tyami.forlaism.block.PhotonMachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

@WailaPlugin
public class PhotonJadePlugin implements IWailaPlugin {
    private static final ResourceLocation ID = new ResourceLocation("forlaism", "photon_info");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PhotonProvider.INSTANCE, PhotonMachineBlock.class);
    }

    private enum PhotonProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, @Nullable IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof PhotonMachineBlockEntity machine) {
                tooltip.add(Component.translatable("tooltip.forlaism.photon_amount",
                        format(machine.getPhotons()), format(machine.getPhotonCapacity())));
            }
        }

        private static String format(long microEph) {
            double value = microEph;
            String[] units = { "μEph", "mEph", "Eph", "KEph", "MEph", "GEph" };
            int unit = 0;
            while (value >= 1000.0 && unit < units.length - 1) { value /= 1000.0; unit++; }
            return String.format(Locale.ROOT, "%.3f %s", value, units[unit]);
        }
    }
}
