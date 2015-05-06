package crazypants.enderio.item.darksteel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;
import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIOTab;
import crazypants.enderio.config.Config;
import crazypants.enderio.gui.IAdvancedTooltipProvider;
import crazypants.enderio.item.darksteel.upgrade.EnergyUpgrade;
import crazypants.enderio.machine.farm.farmers.HarvestResult;
import crazypants.enderio.machine.farm.farmers.TreeHarvestUtil;
import crazypants.util.BlockCoord;
import crazypants.util.ItemUtil;
import crazypants.util.Lang;

public class ItemDarkSteelAxe extends ItemAxe implements IEnergyContainerItem, IAdvancedTooltipProvider, IDarkSteelItem {

  public static boolean isEquipped(EntityPlayer player) {
    if(player == null) {
      return false;
    }
    ItemStack equipped = player.getCurrentEquippedItem();
    if(equipped == null) {
      return false;
    }
    return equipped.getItem() == DarkSteelItems.itemDarkSteelAxe;
  }

  public static boolean isEquippedAndPowered(EntityPlayer player, int requiredPower) {
    return getStoredPower(player) > requiredPower;
  }

  public static int getStoredPower(EntityPlayer player) {
    if(!isEquipped(player)) {
      return 0;
    }
    return EnergyUpgrade.getEnergyStored(player.getCurrentEquippedItem());
  }

  public static ItemDarkSteelAxe create() {
    ItemDarkSteelAxe res = new ItemDarkSteelAxe();
    MinecraftForge.EVENT_BUS.register(res);
    res.init();
    return res;
  }

  private int logOreId = -1;
  private final MultiHarvestComparator harvestComparator = new MultiHarvestComparator();
  
  protected ItemDarkSteelAxe() {
    super(ItemDarkSteelSword.MATERIAL);
    setCreativeTab(EnderIOTab.tabEnderIO);
    String str = "darkSteel_axe";
    setUnlocalizedName(str);
    setTextureName("enderIO:" + str);
  }

  @Override
  public int getIngotsRequiredForFullRepair() {
    return 3;  
  }
  
  @Override
  @SideOnly(Side.CLIENT)
  public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List par3List) {
    ItemStack is = new ItemStack(this);   
    par3List.add(is);

    is = new ItemStack(this);
    EnergyUpgrade.EMPOWERED_FOUR.writeToItem(is);
    EnergyUpgrade.setPowerFull(is);    
    par3List.add(is);
  }
  
  @Override
  public boolean isDamaged(ItemStack stack) {
    return false;
  }

  @Override
  public boolean onBlockStartBreak(ItemStack itemstack, int X, int Y, int Z, EntityPlayer player) {
    if (!player.worldObj.isRemote && player.isSneaking()) {
      Block block = player.worldObj.getBlock(X, Y, Z);
      int blockMetadata = player.worldObj.getBlockMetadata(X, Y, Z);
      if (isLog(block, blockMetadata)) {
        int powerStored = EnergyUpgrade.getEnergyStored(itemstack);

        TreeHarvestUtil harvester = new TreeHarvestUtil();
        HarvestResult res = new HarvestResult();
        BlockCoord bc = new BlockCoord(X, Y, Z);
        harvester.harvest(player.worldObj, bc, res);

        List<BlockCoord> sortedTargets = new ArrayList<BlockCoord>(res.getHarvestedBlocks());
        harvestComparator.refPoint = bc;
        Collections.sort(sortedTargets, harvestComparator);

        int maxBlocks = powerStored / Config.darkSteelAxePowerUsePerDamagePointMultiHarvest;
        int numUsedPower = 0;
        for (int i = 0; numUsedPower < maxBlocks && i < sortedTargets.size(); i++) {
          if (doMultiHarvest(player, player.worldObj, sortedTargets.get(i), block, blockMetadata % 4)) {
            numUsedPower++;
          }
        }
        return numUsedPower != 0;
      }
    }
    return false;
  }

  private boolean doMultiHarvest(EntityPlayer player, World worldObj, BlockCoord bc, Block refBlock, int refMeta) {  
    
    Block block = worldObj.getBlock(bc.x, bc.y, bc.z);
    int meta = worldObj.getBlockMetadata(bc.x, bc.y, bc.z);
    
    ArrayList<ItemStack> itemDrops = block.getDrops(worldObj, bc.x, bc.y, bc.z, meta, 0);
    worldObj.setBlockToAir(bc.x, bc.y, bc.z);
    boolean usedPower = false;
    if(itemDrops != null) {
      for (ItemStack stack : itemDrops) {                
        worldObj.spawnEntityInWorld(new EntityItem(worldObj, bc.x + 0.5, bc.y + 0.5, bc.z + 0.5, stack.copy()));                
        if(TreeHarvestUtil.canDropApples(block, meta)) {
          if(worldObj.rand.nextInt(200) == 0) {            
            worldObj.spawnEntityInWorld(new EntityItem(worldObj, bc.x + 0.5, bc.y + 0.5, bc.z + 0.5, new ItemStack(Items.apple)));
          }
        } else if(block == refBlock) { //other wise leaves
          extractEnergy(player.getCurrentEquippedItem(), Config.darkSteelAxePowerUsePerDamagePointMultiHarvest, false);
          usedPower = true;
        }
      }
    }
    return usedPower;
  }

  @SubscribeEvent
  public void onBreakSpeedEvent(PlayerEvent.BreakSpeed evt) {
    if(evt.entityPlayer.isSneaking() && isEquippedAndPowered(evt.entityPlayer, Config.darkSteelAxePowerUsePerDamagePointMultiHarvest) && isLog(evt.block, evt.metadata)) {
      evt.newSpeed = evt.originalSpeed / Config.darkSteelAxeSpeedPenaltyMultiHarvest;
    }
    if(isEquipped(evt.entityPlayer) && evt.block.getMaterial() == Material.leaves) {
      evt.newSpeed = 6;
    }
  }

  @Override
  public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10) {
    if (world.isRemote) {
      return ItemDarkSteelPickaxe.doRightClickItemPlace(player, world, x, y, z, side, par8, par9, par10);
    }
    return false;
  }

  @Override
  public void setDamage(ItemStack stack, int newDamage) {
    int oldDamage = getDamage(stack);
    if (newDamage <= oldDamage) {
      super.setDamage(stack, newDamage);
    } else {
      int damage = newDamage - oldDamage;
  
      if (!absorbDamageWithEnergy(stack, damage * Config.darkSteelAxePowerUsePerDamagePoint)) {
        super.setDamage(stack, newDamage);
      }
    }
  }

  private boolean absorbDamageWithEnergy(ItemStack stack, int amount) {
    EnergyUpgrade eu = EnergyUpgrade.loadFromItem(stack);
    if(eu != null && eu.isAbsorbDamageWithPower(stack) && eu.getEnergy() > 0) {
      eu.extractEnergy(amount, false);
      eu.writeToItem(stack);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public float getDigSpeed(ItemStack stack, Block block, int meta) {
    if(ForgeHooks.isToolEffective(stack, block, meta)) {
      if(Config.darkSteelPickPowerUsePerDamagePoint <= 0 || getEnergyStored(stack) > 0) {
        return ItemDarkSteelSword.MATERIAL.getEfficiencyOnProperMaterial() + Config.darkSteelAxeEffeciencyBoostWhenPowered;
      }
      return ItemDarkSteelSword.MATERIAL.getEfficiencyOnProperMaterial();
    }
    return super.getDigSpeed(stack, block, meta);
  }

  private boolean isLog(Block block, int meta) {
    if(logOreId == -1) {
      logOreId = OreDictionary.getOreID("logWood");      
    }
    int targetOreId = OreDictionary.getOreID(new ItemStack(block, 1, meta));
    //NB: Specifying the wildcard as meta is a work around for forge issue #1103
    int workAroundId = OreDictionary.getOreID(new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
    return  targetOreId == logOreId || workAroundId == logOreId;
  }

  protected void init() {
    GameRegistry.registerItem(this, getUnlocalizedName());
  }

  @Override
  public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
    return EnergyUpgrade.receiveEnergy(container, maxReceive, simulate);
  }

  @Override
  public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
    return EnergyUpgrade.extractEnergy(container, maxExtract, simulate);
  }

  @Override
  public int getEnergyStored(ItemStack container) {
    return EnergyUpgrade.getEnergyStored(container);
  }

  @Override
  public int getMaxEnergyStored(ItemStack container) {
    return EnergyUpgrade.getMaxEnergyStored(container);
  }

  @Override
  public boolean getIsRepairable(ItemStack i1, ItemStack i2) {
    //return i2 != null && i2.getItem() == EnderIO.itemAlloy && i2.getItemDamage() == Alloy.DARK_STEEL.ordinal();
    return false;
  }

  @Override
  public void addCommonEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    DarkSteelRecipeManager.instance.addCommonTooltipEntries(itemstack, entityplayer, list, flag);
  }

  @Override
  public void addBasicEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    DarkSteelRecipeManager.instance.addBasicTooltipEntries(itemstack, entityplayer, list, flag);
  }

  @Override
  public void addDetailedEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag) {
    if(!Config.addDurabilityTootip) {
      list.add(ItemUtil.getDurabilityString(itemstack));
    }
    String str = EnergyUpgrade.getStoredEnergyString(itemstack);
    if(str != null) {
      list.add(str);
    }
    if(EnergyUpgrade.itemHasAnyPowerUpgrade(itemstack)) {
      list.add(Lang.localize("item.darkSteel_axe.tooltip.multiHarvest"));
      list.add(EnumChatFormatting.WHITE + "+" + Config.darkSteelAxeEffeciencyBoostWhenPowered + " "
          + Lang.localize("item.darkSteel_pickaxe.tooltip.effPowered"));
    }
    DarkSteelRecipeManager.instance.addAdvancedTooltipEntries(itemstack, entityplayer, list, flag);
  }

  public ItemStack createItemStack() {
    return new ItemStack(this);
  }

  private static class MultiHarvestComparator implements Comparator<BlockCoord> {

    BlockCoord refPoint;

    @Override
    public int compare(BlockCoord arg0, BlockCoord arg1) {
      int d1 = refPoint.distanceSquared(arg0);
      int d2 = refPoint.distanceSquared(arg1);
      return compare(d1, d2);
    }

    //NB: Copy of Integer.compare, which i sonly in Java 1.7+
    public static int compare(int x, int y) {
      return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

  }

}
