package net.develgao.beaconchain.datastructures.BeaconChainBlocks;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt384;
import net.develgao.beaconchain.util.uint.UInt64;

public class BeaconBlock {

  // Header
  private UInt64 slot;
  private Hash[] ancestor_hashes;
  private Hash state_root;
  private Hash randao_reveal;
  private Hash candidate_pow_receipt_root;
  private UInt384[] signature;

  // Body
  private BeaconBlockBody body;


  public BeaconBlock() {

  }
}
