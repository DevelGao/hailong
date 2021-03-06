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

package tech.devgao.hailong.datastructures.validator;

import java.util.Optional;
import tech.devgao.hailong.datastructures.state.Committee;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;

public class AttesterInformation {

  private final int validatorIndex;
  private final BLSPublicKey publicKey;
  private final int indexIntoCommitee;
  private final Committee committee;
  private final Optional<BLSSignature> selection_proof;

  public AttesterInformation(
      int validatorIndex,
      BLSPublicKey publicKey,
      int indexIntoCommitee,
      Committee committee,
      Optional<BLSSignature> selection_proof) {
    this.validatorIndex = validatorIndex;
    this.publicKey = publicKey;
    this.indexIntoCommitee = indexIntoCommitee;
    this.committee = committee;
    this.selection_proof = selection_proof;
  }

  public int getValidatorIndex() {
    return validatorIndex;
  }

  public BLSPublicKey getPublicKey() {
    return publicKey;
  }

  public int getIndexIntoCommitee() {
    return indexIntoCommitee;
  }

  public Committee getCommittee() {
    return committee;
  }

  public Optional<BLSSignature> getSelection_proof() {
    return selection_proof;
  }
}
