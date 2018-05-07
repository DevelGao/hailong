package net.develgao.beaconchain.datastructures.BeaconChainOperations;

import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.util.uint.UInt64;

public class Deposit {

  private Hash[] merkle_branch;
  private UInt64 merkle_tree_index;

  //     # Deposit data
  //    'deposit_data': {
  //        # Deposit parameters
  //        'deposit_parameters': DepositParametersRecord,
  //        # Value in Gwei
  //        'value': 'uint64',
  //        # Timestamp from deposit contract
  //        'timestamp': 'uint64',
  //    },

  public Deposit() {

  }

}
