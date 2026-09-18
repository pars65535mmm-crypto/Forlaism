package com.tyami.forlaism.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.AlloyMachineBlockEntity;
import com.tyami.forlaism.block.entity.ConcentratorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Optional;

public class Tier2MachineScreen extends AbstractContainerScreen<Tier2MachineMenu> {
    private static final ResourceLocation ALLOY = new ResourceLocation(Forlaism.MOD_ID, "textures/gui/container/alloy_machine.png");
    private static final ResourceLocation CONCENTRATOR = new ResourceLocation(Forlaism.MOD_ID, "textures/gui/container/concentrator.png");
    private static final ResourceLocation REACTOR = new ResourceLocation(Forlaism.MOD_ID, "textures/gui/container/reactor.png");
    public Tier2MachineScreen(Tier2MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    private ResourceLocation texture() { if (menu.getBlockEntity() instanceof AlloyMachineBlockEntity) return ALLOY; if (menu.getBlockEntity() instanceof ConcentratorBlockEntity) return CONCENTRATOR; return REACTOR; }
    @Override protected void init() { super.init(); inventoryLabelY = 72; titleLabelY = 6; }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        RenderSystem.setShader(GameRenderer::getPositionTexShader); RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.setShaderTexture(0, texture);
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        graphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);
        graphics.fill(x + 16, y + 68 - menu.getScaledEnergy(), x + 24, y + 68, 0xFFFF2222);
        graphics.fill(x + 152, y + 68 - menu.getScaledFluid(), x + 160, y + 68, 0xFF00E5FF);
        if (menu.isCrafting()) graphics.blit(texture, x + 79, y + 65, 176, 14, menu.getScaledProgress() + 1, 16);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, delta); renderTooltip(graphics, mouseX, mouseY);
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        if (mouseX >= x + 16 && mouseX <= x + 24 && mouseY >= y + 16 && mouseY <= y + 68) graphics.renderTooltip(font, List.of(Component.literal("FE: " + menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " FE")), Optional.empty(), mouseX, mouseY);
        if (mouseX >= x + 152 && mouseX <= x + 160 && mouseY >= y + 16 && mouseY <= y + 68) graphics.renderTooltip(font, List.of(Component.literal("FO: " + menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB")), Optional.empty(), mouseX, mouseY);
    }
}
