package crazypants.enderio.item.skull;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

import crazypants.enderio.EnderIO;
import crazypants.enderio.machine.generator.zombie.TileZombieGenerator;
import crazypants.render.BoundingBox;
import crazypants.render.CubeRenderer;
import crazypants.render.IconUtil;
import crazypants.render.RenderUtil;
import crazypants.render.VertexRotation;
import crazypants.vecmath.Vector3d;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelEnderman;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderEnderman;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.ForgeDirection;

public class EndermanSkullRenderer implements ISimpleBlockRenderingHandler, IItemRenderer {

  public EndermanSkullRenderer() {   
  }

  @Override
  public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
    System.out.println("EndermanSkullRenderer.renderInventoryBlock: ");

    renderWorldBlock(null, 0, 0, 0, block, modelId, renderer);
  }


  @Override
  public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
    int meta = 0;
    if(world != null) {
      meta = world.getBlockMetadata(x, y, z);
    }
    return renderWorldBlock(world, x, y, z, block, modelId, renderer, meta);
  }


  public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer, int meta) {
    Tessellator tes = Tessellator.instance;
    tes.addTranslation(x, y, z);
    tes.setColorOpaque_F(1, 1, 1);

    int brightness;
    if(world == null) {
      brightness = 15 << 20 | 15 << 4;
    } else {
      brightness = world.getLightBrightnessForSkyBlocks(x, y, z, 0);
    }
    tes.setBrightness(brightness);


    IIcon[] icons = new IIcon[6];
    for(int i=0;i<icons.length;i++) {      
      icons[i] = block.getIcon(i, meta);              
    }

    float yaw = 180;
    if(world != null) {
      TileEntity te = world.getTileEntity(x, y, z);
      if(te instanceof TileEndermanSkull) {
        yaw = ((TileEndermanSkull)te).yaw;
      }
    }

    VertexRotation rot = new VertexRotation(Math.toRadians(yaw), new Vector3d(0,1,0), new Vector3d(0.5,0,0.5));

    float size = 0.25f;
    float height = 0.5f;
    BoundingBox bb = new BoundingBox(size, 0, size, 1 - size, height, 1 - size); 
    if (renderer.hasOverrideBlockTexture()) {
      CubeRenderer.render(bb, renderer.overrideBlockTexture, rot);
    } else {
      CubeRenderer.render(bb, icons, rot, true);

      if(meta > 1) {
        renderBolts(rot, size);
      }
    }

    tes.addTranslation(-x, -y, -z);

    return true;
  }

  private void renderBolts(VertexRotation rot, float size) {
    BoundingBox bb;
    float boltSize = size/3;
    BoundingBox baseBolt = BoundingBox.UNIT_CUBE.scale(boltSize ,boltSize , boltSize);
    IIcon icon = EnderIO.blockSoulFuser.getIcon(ForgeDirection.EAST.ordinal(), 0);

    float offset = 0.15f;
    bb = baseBolt.translate(size + boltSize/2, -0.15f, offset);
    CubeRenderer.render(bb, icon, rot, true);
    bb = baseBolt.translate(size + boltSize/2, -0.15f, -offset);
    CubeRenderer.render(bb, icon, rot, true);

    bb = baseBolt.translate( -(size + boltSize/2), -0.15f, offset);
    CubeRenderer.render(bb, icon, rot, true);
    bb = baseBolt.translate( -(size + boltSize/2), -0.15f, -offset);
    CubeRenderer.render(bb, icon, rot, true);


    bb = baseBolt.translate( -offset, -0.15f, -(size + boltSize/2));
    CubeRenderer.render(bb, icon, rot, true);
    bb = baseBolt.translate( offset, -0.15f, -(size + boltSize/2));
    CubeRenderer.render(bb, icon, rot, true);

    bb = baseBolt.translate( offset, -0.15f, (size + boltSize/2)); 
    CubeRenderer.render(bb, icon, rot, true);
    bb = baseBolt.translate( -offset, -0.15f, (size + boltSize/2)); 
    CubeRenderer.render(bb, icon, rot, true);
  }

  @Override
  public boolean shouldRender3DInInventory(int modelId) {
    return true;
  }

  @Override
  public int getRenderId() {
    return BlockEndermanSkull.renderId;
  }

  @Override
  public boolean handleRenderType(ItemStack item, ItemRenderType type) {
    return true;
  }

  @Override
  public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {    
    return true;
  }

  @Override
  public void renderItem(ItemRenderType type, ItemStack item, Object... data) {    
    Tessellator.instance.startDrawingQuads();
    GL11.glPushMatrix();
    float scale = 2.5f;    
    float xTrans = -0.5f;
    float yTrans = 0;
    float zTrans = -0.5f;    
    if(type == ItemRenderType.INVENTORY) {
      yTrans = -0.25f;
      scale = 1.8f;
      GL11.glRotatef(180, 0, 1, 0);
      GL11.glRotatef(-90, 0, 1, 0);
    } else if(type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
      scale = 1.8f;
      yTrans = 0f;
      zTrans = -0.25f;
      xTrans = -0.25f;
    }
    GL11.glScalef(scale, scale, scale);
    GL11.glTranslatef(xTrans, yTrans, zTrans);
    RenderUtil.bindBlockTexture();
    renderWorldBlock(null, 0, 0, 0, EnderIO.blockEndermanSkull, getRenderId(), (RenderBlocks)data[0], item.getItemDamage());

    Tessellator.instance.draw();
    GL11.glPopMatrix();

  }

}
