package net.develgao.beaconchain.datastructures.BeaconChainState;

import net.develgao.beaconchain.util.uint.UInt64;

public class ShardReassignmentRecord {

  private int validator_index;
  private UInt64 shard;
  private UInt64 slot;

  public ShardReassignmentRecord() {

  }

}
