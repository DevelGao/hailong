package net.develgao.beaconchain.datastructures;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt64;

public class AttestationData {

  public UInt64 slot;
  public UInt64 shard;
  private Hash beacon_block_hash;
  private Hash epoch_boundary_hash;
  private Hash shard_block_hash;
  private Hash last_crosslink_hash;
  private UInt64 justified_slot;
  private Hash justified_block_hash;

  public AttestationData() {

  }

}
