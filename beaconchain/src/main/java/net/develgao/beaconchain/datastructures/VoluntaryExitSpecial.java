package net.develgao.beaconchain.datastructures;

import net.develgao.beaconchain.util.uint.UInt384;
import net.develgao.beaconchain.util.uint.UInt64;

public class VoluntaryExitSpecial {

  private UInt64 slot;
  private UInt64 validator_index;
  private UInt384[] signature;

  public VoluntaryExitSpecial() {

  }

}
