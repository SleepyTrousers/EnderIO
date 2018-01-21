package crazypants.enderio.base.item.darksteel;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemDoor;

public class ItemDarkSteelDoor extends ItemDoor {

  public ItemDarkSteelDoor(@Nonnull Block block) {
    super(block);
    setUnlocalizedName(block.getUnlocalizedName());

  }

}