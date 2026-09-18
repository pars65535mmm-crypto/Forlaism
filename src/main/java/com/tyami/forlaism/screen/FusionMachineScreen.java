package com.tyami.forlaism.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tyami.forlaism.Forlaism;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class FusionMachineScreen extends AbstractContainerScreen<FusionMachineMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Forlaism.MOD_ID, "textures/gui/container/fusion_machine.png");

    public FusionMachineScreen(FusionMachineMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 72;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // FE バー (左側: x+12, y+18..70)
        int energyScaled = menu.getScaledEnergy();
        guiGraphics.fill(x + 16, y + 68 - energyScaled, x + 24, y + 68, 0xFFFF2222);

        // FO 流体バー (右側: x+150, y+18..70)
        int fluidScaled = menu.getScaledFluid();
        guiGraphics.fill(x + 152, y + 68 - fluidScaled, x + 160, y + 68, 0xFF00E5FF);

        // プログレスバー（中央矢印）
        if (menu.isCrafting()) {
            int progressScaled = menu.getScaledProgress();
            guiGraphics.blit(TEXTURE, x + 79, y + 65, 176, 14, progressScaled + 1, 16);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // FE ツールチップ
        if (mouseX >= x + 16 && mouseX <= x + 24 && mouseY >= y + 16 && mouseY <= y + 68) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.literal("FE: " + menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " FE")
            ), Optional.empty(), mouseX, mouseY);
        }

        // FO ツールチップ
        if (mouseX >= x + 152 && mouseX <= x + 160 && mouseY >= y + 16 && mouseY <= y + 68) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.literal("FO: " + menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        }
    }
}
