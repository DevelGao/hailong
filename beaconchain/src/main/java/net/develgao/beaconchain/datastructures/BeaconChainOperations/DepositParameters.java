package net.develgao.beaconchain.datastructures.BeaconChainOperations;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt384;

public class DepositParameters {

  private UInt384 pubkey;
  private UInt384[] proof_of_possession;
  private Hash withdrawal_credentials;
  private Hash randao_commitment;

  public DepositParameters() {

  }
}
