package net.develgao.beaconchain.datastructures.BeaconChainBlocks;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt64;

public class ProposalSignedData {

  private UInt64 slot;
  private UInt64 shard;
  private Hash block_hash;

  public ProposalSignedData() {

  }

}
