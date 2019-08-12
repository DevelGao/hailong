/*
 * Copyright 2019 Developer Gao.
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

package tech.devgao.hailong.datastructures.blocks;

import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.operations.AttesterSlashing;
import tech.devgao.hailong.datastructures.operations.Deposit;
import tech.devgao.hailong.datastructures.operations.ProposerSlashing;
import tech.devgao.hailong.datastructures.operations.SignedVoluntaryExit;
import tech.devgao.hailong.util.SSZTypes.SSZList;
import tech.devgao.hailong.util.config.Constants;

public class BeaconBlockBodyLists {

  public static SSZList<ProposerSlashing> createProposerSlashings() {
    return new SSZList<>(ProposerSlashing.class, Constants.MAX_PROPOSER_SLASHINGS);
  }

  public static SSZList<AttesterSlashing> createAttesterSlashings() {
    return new SSZList<>(AttesterSlashing.class, Constants.MAX_ATTESTER_SLASHINGS);
  }

  public static SSZList<Attestation> createAttestations() {
    return new SSZList<>(Attestation.class, Constants.MAX_ATTESTATIONS);
  }

  public static SSZList<Deposit> createDeposits() {
    return new SSZList<>(Deposit.class, Constants.MAX_DEPOSITS);
  }

  public static SSZList<SignedVoluntaryExit> createVoluntaryExits() {
    return new SSZList<>(SignedVoluntaryExit.class, Constants.MAX_VOLUNTARY_EXITS);
  }
}
