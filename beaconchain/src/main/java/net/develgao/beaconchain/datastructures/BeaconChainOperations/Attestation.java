package net.develgao.beaconchain.datastructures.BeaconChainOperations;

import net.develgao.beaconchain.util.bytes.Bytes32;
import net.develgao.beaconchain.util.uint.UInt384;


public class Attestation {

  private AttestationData data;
  private Bytes32 participation_bitfield;
  private Bytes32 custody_bitfield;
  private UInt384 aggregate_signature;

  public Attestation() {

  }

}
