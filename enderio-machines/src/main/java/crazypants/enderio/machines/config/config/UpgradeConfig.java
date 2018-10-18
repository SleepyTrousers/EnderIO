package crazypants.enderio.machines.config.config;

import com.enderio.core.common.util.NNList;

import crazypants.enderio.base.config.factory.IValue;
import crazypants.enderio.base.config.factory.IValueFactory;
import crazypants.enderio.machines.config.Config;

public class UpgradeConfig {

  public static final IValueFactory F = Config.F.section("upgrades");

  public static final IValueFactory F_SOLAR = F.section(".solar");

  public static final NNList<IValue<Integer>> solarPowerGen = new NNList<>(
      F_SOLAR.make("powerGen1", 10, "Energy per SECOND generated by the Solar I upgrade. Split between all equipped DS armors.").setMin(1).sync(),
      F_SOLAR.make("powerGen2", 40, "Energy per SECOND generated by the Solar II upgrade. Split between all equipped DS armors.").setMin(1).sync(),
      F_SOLAR.make("powerGen3", 160, "Energy per SECOND generated by the Solar III upgrade. Split between all equipped DS armors.").setMin(1).sync());

  public static final NNList<IValue<Integer>> solarUpradeCost = new NNList<>(
      F_SOLAR.make("upgradeCost1", 4, "Cost in XP levels of the Solar I upgrade.").setMin(1).sync(),
      F_SOLAR.make("upgradeCost2", 8, "Cost in XP levels of the Solar II upgrade.").setMin(1).sync(),
      F_SOLAR.make("upgradeCost3", 24, "Cost in XP levels of the Solar III upgrade.").setMin(1).sync());

  public static final IValue<Boolean> helmetChargeOthers = F_SOLAR.make("chargeOthers", true, //
      "If enabled allows the solar upgrade to charge non-darksteel armors that the player is wearing.").sync();
  
  public static final IValueFactory F_WET = F.section(".wet");

  public static final IValue<Integer> wetCost = F_WET.make("upgradeCost", 6, "Cost in XP levels of the Wet upgrade").setMin(1).sync();
  public static final IValue<Integer> wetEnergyUsePerCobblestoneConverstion = F_WET.make("energyUsePerCobblestoneConversiton", 400, //
      "How much energy the upgrade will use per flowing block of lava converted to cobblestone.").setMin(1).sync();
  public static final IValue<Integer> wetEnergyUsePerObsidianConverstion = F_WET.make("energyUsePerObsidianConversiton", 2000, //
      "How much energy the upgrade will use per source block of lava converted to obsidian.").setMin(1).sync();
  public static final IValue<Double> wetRange = F_WET.make("range", 2.0D, //
      "How far away from the player blocks of lava will be converted to obsidian or cobblestone.").setRange(0, 8).sync();

}
