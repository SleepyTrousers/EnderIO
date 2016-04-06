package crazypants.enderio.machine.soul;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.enderio.core.api.common.util.ITankAccess;
import com.enderio.core.common.util.FluidUtil;

import crazypants.enderio.ModObject;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.AbstractPoweredTaskEntity;
import crazypants.enderio.machine.IMachineRecipe;
import crazypants.enderio.machine.MachineRecipeInput;
import crazypants.enderio.machine.MachineRecipeRegistry;
import crazypants.enderio.machine.SlotDefinition;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.paint.IPaintable;
import crazypants.enderio.xp.ExperienceContainer;
import crazypants.enderio.xp.IHaveExperience;
import crazypants.enderio.xp.PacketExperianceContainer;
import crazypants.enderio.xp.XpUtil;

import static crazypants.enderio.capacitor.CapacitorKey.SOUL_BINDER_POWER_BUFFER;
import static crazypants.enderio.capacitor.CapacitorKey.SOUL_BINDER_POWER_INTAKE;
import static crazypants.enderio.capacitor.CapacitorKey.SOUL_BINDER_POWER_USE;

public class TileSoulBinder extends AbstractPoweredTaskEntity implements IHaveExperience, IFluidHandler, ITankAccess, IPaintable.IPaintableTileEntity {

  private final ExperienceContainer xpCont = new ExperienceContainer(XpUtil.getExperienceForLevel(Config.soulBinderMaxXpLevel));

  public TileSoulBinder() {
    super(new SlotDefinition(2, 2, 1), SOUL_BINDER_POWER_INTAKE, SOUL_BINDER_POWER_BUFFER, SOUL_BINDER_POWER_USE);
  }

  @Override
  public ExperienceContainer getContainer() {  
    return xpCont;
  }

  @Override
  public String getMachineName() {    
    return ModObject.blockSoulBinder.getUnlocalisedName();
  }
    
  @Override
  public int getInventoryStackLimit() {
    return 1;
  }
  
  @Override
  protected boolean processTasks(boolean redstoneChecksPassed) {
    if(xpCont.isDirty()) {
      PacketHandler.sendToAllAround(new PacketExperianceContainer(this), this);
      xpCont.setDirty(false);
    }
    return super.processTasks(redstoneChecksPassed);
  }

  @Override
  protected IMachineRecipe canStartNextTask(float chance) {
    IMachineRecipe recipe = super.canStartNextTask(chance);
    if(recipe == null) {
      return null;
    }
    int xpRequired = ((ISoulBinderRecipe)recipe).getExperienceRequired();
    if(xpCont.getExperienceTotal() >= xpRequired) {
      return recipe;
    }
    return null;
  }

  public boolean needsXP() {
    return getXPRequired() > 0;
  }

  /**
   * Computes the required amount of XP to start the current recipe.
   * @return 0 if no XP is required, negative when more than required XP is stored.
   */
  private int getXPRequired() {
    if(currentTask != null) {
      return 0;
    }
    IMachineRecipe nextRecipe = getNextRecipe();
    if(! (nextRecipe instanceof ISoulBinderRecipe)) {
      return 0;
    }
    return ((ISoulBinderRecipe)nextRecipe).getExperienceRequired() - getContainer().getExperienceTotal();
  }

  public int getCurrentlyRequiredLevel() {
    if(currentTask != null) {
      return -1;
    }
    IMachineRecipe nextRecipe = getNextRecipe();
    if(! (nextRecipe instanceof ISoulBinderRecipe)) {
      return -1;
    }
    return ((ISoulBinderRecipe)nextRecipe).getExperienceLevelsRequired();
  }
  

  @Override
  protected boolean startNextTask(IMachineRecipe nextRecipe, float chance) {
    int xpRequired = ((ISoulBinderRecipe)nextRecipe).getExperienceRequired();
    if(xpCont.getExperienceTotal() < xpRequired) {
      return false;
    }
    if(super.startNextTask(nextRecipe, chance)) {
      xpCont.drain(null, XpUtil.experienceToLiquid(xpRequired), true);
      return true;
    }
    return false;
  }

  @Override
  protected boolean isMachineItemValidForSlot(int slot, ItemStack item) {
    if(!slotDefinition.isInputSlot(slot)) {
      return false;
    }
    MachineRecipeInput newInput = new MachineRecipeInput(slot, item);
    int otherSlot = slot == 0 ? 1 : 0;    
    if(inventory[otherSlot] == null) {
      List<IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForInput(getMachineName(), newInput);
      if(recipes.isEmpty()) {
        return false;
      }    
      for(IMachineRecipe rec : recipes) {
        if(rec != null && rec.isValidInput(newInput)) {
          return true;
        }
      }  
    } else {
      MachineRecipeInput[] inputs = new MachineRecipeInput[] {
          newInput,
          new MachineRecipeInput(otherSlot, inventory[otherSlot])
      };
      return MachineRecipeRegistry.instance.getRecipeForInputs(getMachineName(), inputs) != null;
    }
    return false;
  }

  @Override
  protected boolean doPull(EnumFacing dir) {
    boolean res = super.doPull(dir);
    int req = getXPRequired();
    if(req > 0) {
      FluidUtil.doPull(this, dir, Math.min(XpUtil.experienceToLiquid(req), Config.fluidConduitExtractRate));
    }
    return res;
  }
  
  @Override
  protected boolean doPush(EnumFacing dir) {
    boolean res = super.doPush(dir);
    int maxAmount = Math.min(XpUtil.experienceToLiquid(getExcessXP()), Config.fluidConduitExtractRate);
    if (maxAmount > 0) {
      FluidUtil.doPush(this, dir, maxAmount);
    }
    return res;
  }

  /**
   * Determines how much stored XP can/should be removed because it is not
   * needed for the next recipe.
   * 
   * @return A number between 0 and the amount of stored XP
   */
  private int getExcessXP() {
    if (currentTask == null) {
      IMachineRecipe nextRecipe = getNextRecipe();
      if (nextRecipe instanceof ISoulBinderRecipe) {
        return Math.max(0, getContainer().getExperienceTotal() - ((ISoulBinderRecipe) nextRecipe).getExperienceRequired());
      }
    }
    return getContainer().getExperienceTotal();
  }

  @Override
  public boolean canFill(EnumFacing from, Fluid fluid) {    
    return xpCont.canFill(from, fluid);
  }
  
  @Override
  public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
    int max = XpUtil.experienceToLiquid(getXPRequired());
    if (resource == null || max <= 0) {
      return 0;
    } else if (max < resource.amount) {
      FluidStack copy = resource.copy();
      copy.amount = max;
      return xpCont.fill(from, copy, doFill);
    } else {
      return xpCont.fill(from, resource, doFill);
    }
  }

  @Override
  public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
    int max = XpUtil.experienceToLiquid(getExcessXP());
    if (resource != null && max < resource.amount) {
      FluidStack copy = resource.copy();
      copy.amount = max;
      return xpCont.drain(from, copy, doDrain);
    } else {
      return xpCont.drain(from, resource, doDrain);
    }
  }

  @Override
  public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {    
    return xpCont.drain(from, Math.min(XpUtil.experienceToLiquid(getExcessXP()), maxDrain), doDrain);
  }

  @Override
  public boolean canDrain(EnumFacing from, Fluid fluid) {    
    return xpCont.canDrain(from, fluid);
  }

  @Override
  public FluidTankInfo[] getTankInfo(EnumFacing from) {    
    return xpCont.getTankInfo(from);
  }

  @Override
  public void readCommon(NBTTagCompound nbtRoot) {
    super.readCommon(nbtRoot);
    xpCont.readFromNBT(nbtRoot);
  }

  @Override
  public void writeCommon(NBTTagCompound nbtRoot) { 
    super.writeCommon(nbtRoot);
    xpCont.writeToNBT(nbtRoot);
  }
  
  @Override
  public FluidTank getInputTank(FluidStack forFluidType) {
    return xpCont;
  }

  @Override
  public FluidTank[] getOutputTanks() {
    return new FluidTank[] { xpCont };
  }

  @Override
  public void setTanksDirty() {
    xpCont.setDirty(true);
  }
  
}
