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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes48;
import net.develgao.cava.ssz.SSZ;
import tech.devgao.artemis.datastructures.blocks.ProposalSignedData;

public class ProposerSlashing {

  private int proposer_index;
  private ProposalSignedData proposal_data_1;
  private List<Bytes48> proposal_signature_1;
  private ProposalSignedData proposal_data_2;
  private List<Bytes48> proposal_signature_2;

  public ProposerSlashing(
      int proposer_index,
      ProposalSignedData proposal_data_1,
      List<Bytes48> proposal_signature_1,
      ProposalSignedData proposal_data_2,
      List<Bytes48> proposal_signature_2) {
    this.proposer_index = proposer_index;
    this.proposal_data_1 = proposal_data_1;
    this.proposal_signature_1 = proposal_signature_1;
    this.proposal_data_2 = proposal_data_2;
    this.proposal_signature_2 = proposal_signature_2;
  }

  public static ProposerSlashing fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new ProposerSlashing(
                reader.readInt(24),
                ProposalSignedData.fromBytes(reader.readBytes()),
                reader.readBytesList().stream().map(Bytes48::wrap).collect(Collectors.toList()),
                ProposalSignedData.fromBytes(reader.readBytes()),
                reader.readBytesList().stream().map(Bytes48::wrap).collect(Collectors.toList())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeInt(proposer_index, 24);
          writer.writeBytes(proposal_data_1.toBytes());
          writer.writeBytesList(proposal_signature_1);
          writer.writeBytes(proposal_data_2.toBytes());
          writer.writeBytesList(proposal_signature_2);
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        proposer_index,
        proposal_data_1,
        proposal_signature_1,
        proposal_data_2,
        proposal_signature_2);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ProposerSlashing)) {
      return false;
    }

    ProposerSlashing other = (ProposerSlashing) obj;
    return Objects.equals(this.getProposer_index(), other.getProposer_index())
        && Objects.equals(this.getProposal_data_1(), other.getProposal_data_1())
        && Objects.equals(this.getProposal_signature_1(), other.getProposal_signature_1())
        && Objects.equals(this.getProposal_data_2(), other.getProposal_data_2())
        && Objects.equals(this.getProposal_signature_2(), other.getProposal_signature_2());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public int getProposer_index() {
    return proposer_index;
  }

  public void setProposer_index(int proposer_index) {
    this.proposer_index = proposer_index;
  }

  public ProposalSignedData getProposal_data_1() {
    return proposal_data_1;
  }

  public void setProposal_data_1(ProposalSignedData proposal_data_1) {
    this.proposal_data_1 = proposal_data_1;
  }

  public List<Bytes48> getProposal_signature_1() {
    return proposal_signature_1;
  }

  public void setProposal_signature_1(List<Bytes48> proposal_signature_1) {
    this.proposal_signature_1 = proposal_signature_1;
  }

  public ProposalSignedData getProposal_data_2() {
    return proposal_data_2;
  }

  public void setProposal_data_2(ProposalSignedData proposal_data_2) {
    this.proposal_data_2 = proposal_data_2;
  }

  public List<Bytes48> getProposal_signature_2() {
    return proposal_signature_2;
  }

  public void setProposal_signature_2(List<Bytes48> proposal_signature_2) {
    this.proposal_signature_2 = proposal_signature_2;
  }
}
