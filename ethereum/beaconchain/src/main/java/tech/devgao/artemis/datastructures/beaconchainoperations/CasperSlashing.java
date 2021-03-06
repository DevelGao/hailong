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

package tech.devgao.artemis.datastructures.beaconchainoperations;

import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.ssz.SSZ;

public class CasperSlashing {

  private SlashableVoteData votes_1;
  private SlashableVoteData votes_2;

  public CasperSlashing(SlashableVoteData votes_1, SlashableVoteData votes_2) {
    this.votes_1 = votes_1;
    this.votes_2 = votes_2;
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(votes_1.toBytes());
          writer.writeBytes(votes_2.toBytes());
        });
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public SlashableVoteData getVotes_1() {
    return votes_1;
  }

  public void setVotes_1(SlashableVoteData votes_1) {
    this.votes_1 = votes_1;
  }

  public SlashableVoteData getVotes_2() {
    return votes_2;
  }

  public void setVotes_2(SlashableVoteData votes_2) {
    this.votes_2 = votes_2;
  }
}
