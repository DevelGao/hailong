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

package tech.devgao.artemis.datastructures.beaconchainstate;

import tech.devgao.artemis.datastructures.beaconchainoperations.AttestationData;
import tech.devgao.artemis.util.bytes.Bytes32;
import tech.devgao.artemis.util.uint.UInt64;

public class PendingAttestationRecord {

  private AttestationData data;
  private Bytes32 participation_bitfield;
  private Bytes32 custody_bitfield;
  private UInt64 slot_included;

  public PendingAttestationRecord(
      AttestationData data,
      Bytes32 participation_bitfield,
      Bytes32 custody_bitfield,
      UInt64 slot_included) {
    this.data = data;
    this.participation_bitfield = participation_bitfield;
    this.custody_bitfield = custody_bitfield;
    this.slot_included = slot_included;
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public AttestationData getData() {
    return data;
  }

  public void setData(AttestationData data) {
    this.data = data;
  }

  public Bytes32 getParticipation_bitfield() {
    return participation_bitfield;
  }

  public void setParticipation_bitfield(Bytes32 participation_bitfield) {
    this.participation_bitfield = participation_bitfield;
  }

  public Bytes32 getCustody_bitfield() {
    return custody_bitfield;
  }

  public void setCustody_bitfield(Bytes32 custody_bitfield) {
    this.custody_bitfield = custody_bitfield;
  }

  public UInt64 getSlot_included() {
    return slot_included;
  }

  public void setSlot_included(UInt64 slot_included) {
    this.slot_included = slot_included;
  }
}
