package net.develgao.beaconchain.datastructures;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt384;

public class DepositParametersRecord {

  private UInt384 pubkey;
  private UInt384[] proof_of_possession;
  private Hash withdrawal_credentials;
  private Hash randao_commitment;

  public DepositParametersRecord() {

  }
}
