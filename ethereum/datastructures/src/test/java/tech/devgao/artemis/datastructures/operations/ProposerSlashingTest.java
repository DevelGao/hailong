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

package tech.devgao.artemis.datastructures.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomInt;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomProposalSignedData;

import java.util.Objects;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes48;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.datastructures.blocks.ProposalSignedData;

class ProposerSlashingTest {

  int proposerIndex = randomInt();
  ProposalSignedData proposalData1 = randomProposalSignedData();
  BLSSignature proposalSignature1 = new BLSSignature(Bytes48.random(), Bytes48.random());
  ProposalSignedData proposalData2 = randomProposalSignedData();
  BLSSignature proposalSignature2 = new BLSSignature(Bytes48.random(), Bytes48.random());

  ProposerSlashing proposerSlashing =
      new ProposerSlashing(
          proposerIndex, proposalData1, proposalSignature1, proposalData2, proposalSignature2);

  @Test
  void equalsReturnsTrueWhenObjectAreSame() {
    ProposerSlashing testProposerSlashing = proposerSlashing;

    assertEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex, proposalData1, proposalSignature1, proposalData2, proposalSignature2);

    assertEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsFalseWhenProposerIndicesAreDifferent() {
    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex + randomInt(),
            proposalData1,
            proposalSignature1,
            proposalData2,
            proposalSignature2);

    assertNotEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsFalseWhenProposalData1IsDifferent() {
    // ProposalSignedData is rather involved to create. Just create a random one until it is not the
    // same as the original.
    ProposalSignedData otherProposalData1 = randomProposalSignedData();
    while (Objects.equals(otherProposalData1, proposalData1)) {
      otherProposalData1 = randomProposalSignedData();
    }

    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex,
            otherProposalData1,
            proposalSignature1,
            proposalData2,
            proposalSignature2);

    assertNotEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsFalseWhenProposalSignature1sAreDifferent() {
    // Create copy of proposalSignature1 and reverse to ensure it is different.
    BLSSignature reverseProposalSignature1 =
        new BLSSignature(proposalSignature1.getC1(), proposalSignature1.getC0());

    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex,
            proposalData1,
            reverseProposalSignature1,
            proposalData2,
            proposalSignature2);

    assertNotEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsFalseWhenProposalData2IsDifferent() {
    // ProposalSignedData is rather involved to create. Just create a random one until it is not the
    // same as the original.
    ProposalSignedData otherProposalData2 = randomProposalSignedData();
    while (Objects.equals(otherProposalData2, proposalData2)) {
      otherProposalData2 = randomProposalSignedData();
    }

    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex,
            proposalData1,
            proposalSignature1,
            otherProposalData2,
            proposalSignature2);

    assertNotEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void equalsReturnsFalseWhenProposalSignature2sAreDifferent() {
    // Create copy of proposalSignature1 and reverse to ensure it is different.
    BLSSignature reverseProposalSignature2 =
        new BLSSignature(proposalSignature2.getC1(), proposalSignature2.getC0());

    ProposerSlashing testProposerSlashing =
        new ProposerSlashing(
            proposerIndex,
            proposalData1,
            proposalSignature1,
            proposalData2,
            reverseProposalSignature2);

    assertNotEquals(proposerSlashing, testProposerSlashing);
  }

  @Test
  void rountripSSZ() {
    Bytes sszProposerSlashingBytes = proposerSlashing.toBytes();
    assertEquals(proposerSlashing, ProposerSlashing.fromBytes(sszProposerSlashingBytes));
  }
}