/*
 * Copyright 2018 Developer Gao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.develgao.beaconchain;

import net.develgao.beaconchain.util.bytes.Bytes32;

public final class Constants {

  // The constants below are correct as of spec dated 2018/12/5

  // Misc
  static final int SHARD_COUNT                                    = (int) Math.pow(2, 10); // 1,024 Shards
  static final int TARGET_COMMITTEE_SIZE                          = (int) Math.pow(2, 8); // 256 validators
  static final int MAX_ATTESTATIONS_PER_BLOCK                     = (int) Math.pow(2, 7);  // 128 attestations
  static final int MIN_BALANCE                                    = (int) Math.pow(2, 4);  // 16 Eth
  static final int MAX_BALANCE_CHURN_QUOTIENT                     = (int) Math.pow(2, 5);  // 32
  static final int GWEI_PER_ETH                                   = (int) Math.pow(10, 9); // 1,000,000,000 Wei
  static final int BEACON_CHAIN_SHARD_NUMBER                      = (int) Math.pow(2, 64) - 1;
  static final String BLS_WITHDRAWAL_CREDENTIALS                  = "0x00";

  // Deposit contract
  //  static final Address DEPOSIT_CONTRACT_ADDRESS               =  Value is still TBD
  static final int DEPOSIT_CONTRACT_TREE_DEPTH                    = (int) Math.pow(2, 5);  // 32
  static final int MIN_DEPOSIT                                    = (int) Math.pow(2, 0);  // 1 Eth
  static final int MAX_DEPOSIT                                    = (int) Math.pow(2, 5);  // 32 Eth

  // Initial values
  static final int INITIAL_FORK_VERSION                           = 0;
  static final int INITIAL_SLOT_NUMBER                            = 0;
  static final Bytes32 ZERO_HASH                                  = Bytes32.ZERO;

  // Time parameters
  static final int SLOT_DURATION                                  = 6;  // 6 seconds
  static final int MIN_ATTESTATION_INCLUSION_DELAY                = (int) Math.pow(2, 2); // 4 slots
  static final int EPOCH_LENGTH                                   = (int) Math.pow(2, 6);  // 64 slots
  static final int MIN_VALIDATOR_REGISTRY_CHANGE_INTERVAL	        = (int) Math.pow(2, 8);  // 256 slots
  static final int POW_RECEIPT_ROOT_VOTING_PERIOD	        	      = (int) Math.pow(2, 10); // 1,024 slots
  static final int SHARD_PERSISTENT_COMMITTEE_CHANGE_PERIOD       = (int) Math.pow(2, 17); // 131,072 slots
  static final int COLLECTIVE_PENALTY_CALCULATION_PERIOD          = (int) Math.pow(2, 20); // 1,048,576 slots
  static final int ZERO_BALANCE_VALIDATOR_TTL                     = (int) Math.pow(2, 22); // 4,194,304 slots

  // Reward and penalty quotients
  static final int BASE_REWARD_QUOTIENT                           = (int) Math.pow(2, 11); // 2,048
  static final int WHISTLEBLOWER_REWARD_QUOTIENT                  = (int) Math.pow(2, 9); // 512
  static final int INCLUDER_REWARD_QUOTIENT                       = (int) Math.pow(2, 3); // 8
  static final int INACTIVITY_PENALTY_QUOTIENT                    = (int) Math.pow(2, 34); // 131,072

  // Status codes
  static final int PENDING_ACTIVATION                             = 0;
  static final int ACTIVE                                         = 1;
  static final int ACTIVE_PENDING_EXIT                            = 2;
  static final int EXITED_WITHOUT_PENALTY                         = 3;
  static final int EXITED_WITH_PENALTY                            = 4;

  // Special record types
  static final int VOLUNTARY_EXIT                                 = 0;
  static final int CASPER_SLASHING                                = 1;
  static final int PROPOSER_SLASHING                              = 2;
  static final int DEPOSIT_PROOF                                  = 3;

  // Validator registry delta flags
  static final int ACTIVATION                                     = 0;
  static final int EXIT                                           = 1;

  // Signature domains
  static final int DOMAIN_DEPOSIT                                 = 0;
  static final int DOMAIN_ATTESTATION                             = 1;
  static final int DOMAIN_PROPOSAL                                = 2;
  static final int DOMAIN_EXIT                                    = 3;

  public static String getConstantsAsString() {
    return "--Misc--"
            + "\nSHARD_COUNT: " + SHARD_COUNT
            + "\nTARGET_COMMITTEE_SIZE: " + TARGET_COMMITTEE_SIZE
            + "\nMAX_ATTESTATIONS_PER_BLOCK: " + MAX_ATTESTATIONS_PER_BLOCK
            + "\nMIN_BALANCE: " + MIN_BALANCE
            + "\nMAX_BALANCE_CHURN_QUOTIENT: " + MAX_BALANCE_CHURN_QUOTIENT
            + "\nGWEI_PER_ETH: " + GWEI_PER_ETH
            + "\nBEACON_CHAIN_SHARD_NUMBER: " + BEACON_CHAIN_SHARD_NUMBER
            + "\nBLS_WITHDRAWAL_CREDENTIALS: " + BLS_WITHDRAWAL_CREDENTIALS

            + "\n\n--Deposit contract--"
//            + "\nDEPOSIT_CONTRACT_ADDRESS: " + DEPOSIT_CONTRACT_ADDRESS
            + "\nDEPOSIT_CONTRACT_TREE_DEPTH: " + DEPOSIT_CONTRACT_TREE_DEPTH
            + "\nMIN_DEPOSIT: " + MIN_DEPOSIT
            + "\nMAX_DEPOSIT: " + MAX_DEPOSIT

            + "\n\n--Initial values--"
            + "\nINITIAL_FORK_VERSION: " + INITIAL_FORK_VERSION
            + "\nINITIAL_SLOT_NUMBER: " + INITIAL_SLOT_NUMBER
            + "\nZERO_HASH: " + ZERO_HASH

            + "\n\n--Time parameters--"
            + "\nSLOT_DURATION: " + SLOT_DURATION
            + "\nMIN_ATTESTATION_INCLUSION_DELAY: " + MIN_ATTESTATION_INCLUSION_DELAY
            + "\nEPOCH_LENGTH: " + EPOCH_LENGTH
            + "\nMIN_VALIDATOR_REGISTRY_CHANGE_INTERVAL: " + MIN_VALIDATOR_REGISTRY_CHANGE_INTERVAL
            + "\nPOW_RECEIPT_ROOT_VOTING_PERIOD: " + POW_RECEIPT_ROOT_VOTING_PERIOD
            + "\nSHARD_PERSISTENT_COMMITTEE_CHANGE_PERIOD: " + SHARD_PERSISTENT_COMMITTEE_CHANGE_PERIOD
            + "\nCOLLECTIVE_PENALTY_CALCULATION_PERIOD: " + COLLECTIVE_PENALTY_CALCULATION_PERIOD
            + "\nZERO_BALANCE_VALIDATOR_TTL: " + ZERO_BALANCE_VALIDATOR_TTL

            + "\n\n--Reward and penalty quotients--"
            + "\nBASE_REWARD_QUOTIENT: " + BASE_REWARD_QUOTIENT
            + "\nWHISTLEBLOWER_REWARD_QUOTIENT: " + WHISTLEBLOWER_REWARD_QUOTIENT
            + "\nINCLUDER_REWARD_QUOTIENT: " + INCLUDER_REWARD_QUOTIENT
            + "\nINACTIVITY_PENALTY_QUOTIENT: " + INACTIVITY_PENALTY_QUOTIENT

            + "\n\n--Status codes--"
            + "\nPENDING_ACTIVATION: " + PENDING_ACTIVATION
            + "\nACTIVE: " + ACTIVE
            + "\nACTIVE_PENDING_EXIT: " + ACTIVE_PENDING_EXIT
            + "\nEXITED_WITHOUT_PENALTY: " + EXITED_WITHOUT_PENALTY
            + "\nEXITED_WITH_PENALTY: " + EXITED_WITH_PENALTY

            + "\n\n--Special record types--"
            + "\nVOLUNTARY_EXIT: " + VOLUNTARY_EXIT
            + "\nCASPER_SLASHING: " + CASPER_SLASHING
            + "\nPROPOSER_SLASHING: " + PROPOSER_SLASHING
            + "\nDEPOSIT_PROOF: " + DEPOSIT_PROOF

            + "\n\n--Validator registry delta flags--"
            + "\nACTIVATION: " + ACTIVATION
            + "\nEXIT: " + EXIT

            + "\n\n--Signature domains--"
            + "\nDOMAIN_DEPOSIT: " + DOMAIN_DEPOSIT
            + "\nDOMAIN_ATTESTATION: " + DOMAIN_ATTESTATION
            + "\nDOMAIN_PROPOSAL: " + DOMAIN_PROPOSAL
            + "\nDOMAIN_EXIT: " + DOMAIN_EXIT
        + "\n";
  }

}
