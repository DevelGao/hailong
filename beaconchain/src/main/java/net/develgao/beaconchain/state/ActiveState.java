package net.develgao.beaconchain.state;

import net.develgao.beaconchain.datastructures.AttestationRecord;
import net.develgao.beaconchain.datastructures.SpecialObject;
import net.develgao.beaconchain.ethereum.core.Hash;

public class ActiveState {

  private AttestationRecord[] pending_attestations;
  private Hash[] recent_block_hashes;
  private SpecialObject[] pending_specials;

  public ActiveState() {

  }

}
