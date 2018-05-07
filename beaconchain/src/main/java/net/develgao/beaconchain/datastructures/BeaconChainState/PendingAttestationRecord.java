package net.develgao.beaconchain.datastructures.BeaconChainState;

import net.develgao.beaconchain.datastructures.BeaconChainOperations.AttestationData;
import net.develgao.beaconchain.util.bytes.Bytes32;
import net.develgao.beaconchain.util.uint.UInt64;

public class PendingAttestationRecord {

  private AttestationData data;
  private Bytes32 participation_bitfield;
  private Bytes32 custody_bitfield;
  private UInt64 slot_included;

  public PendingAttestationRecord() {

  }

}
