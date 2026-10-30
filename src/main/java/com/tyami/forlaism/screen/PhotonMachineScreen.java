package com.tyami.forlaism.screen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.PhotonMachineBlockEntity;
import net.minecraft.client.gui.GuiGraphics; import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen; import net.minecraft.client.renderer.GameRenderer; import net.minecraft.network.chat.Component; import net.minecraft.resources.ResourceLocation; import net.minecraft.world.entity.player.Inventory;
public class PhotonMachineScreen extends AbstractContainerScreen<PhotonMachineMenu> {
 private static final ResourceLocation GUI=new ResourceLocation(Forlaism.MOD_ID,"textures/gui/container/photon_gui.png"), COLLECTOR=new ResourceLocation(Forlaism.MOD_ID,"textures/gui/container/photon_collector_gui.png");
 public PhotonMachineScreen(PhotonMachineMenu m,Inventory i,Component t){super(m,i,t); imageWidth=176; imageHeight=166;}
 @Override protected void init(){super.init(); inventoryLabelY=72; titleLabelY=6;}
 @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){ResourceLocation tex=menu.getBlockEntity().getKind()==PhotonMachineBlockEntity.Kind.COLLECTOR?COLLECTOR:GUI; RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,tex);int x=(width-imageWidth)/2,y=(height-imageHeight)/2;g.blit(tex,x,y,0,0,imageWidth,imageHeight); int e=menu.getMaxEnergy()==0?0:Math.min(54,menu.getEnergy()*54/menu.getMaxEnergy()); int ph=Math.min(54,menu.getPhotons()*54/1000); if(tex==GUI){g.blit(tex,x+20,y+15,20,15,18,54);g.blit(tex,x+134,y+15,134,15,18,54);if(e>0)g.blit(tex,x+20,y+69-e,201,54-e,18,e);if(ph>0)g.blit(tex,x+134,y+69-ph,177,54-ph,18,ph);} else {g.blit(tex,x+80,y+35,167,14,22,16);} }
 @Override public void render(GuiGraphics g,int mx,int my,float d){renderBackground(g);super.render(g,mx,my,d);renderTooltip(g,mx,my);}
}
