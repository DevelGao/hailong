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

package tech.devgao.artemis.datastructures.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomBeaconBlockBody;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomEth1Data;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomLong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.bytes.Bytes48;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.datastructures.operations.BLSSignature;

class BeaconBlockTest {

  long slot = randomLong();
  Bytes32 parentRoot = Bytes32.random();
  Bytes32 stateRoot = Bytes32.random();
  List<Bytes48> randaoReveal = Arrays.asList(Bytes48.random(), Bytes48.random());
  Eth1Data eth1Data = randomEth1Data();
  BLSSignature signature = new BLSSignature(Bytes48.random(), Bytes48.random());
  BeaconBlockBody body = randomBeaconBlockBody();

  BeaconBlock beaconBlock =
      new BeaconBlock(slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);

  @Test
  void equalsReturnsTrueWhenObjectAreSame() {
    BeaconBlock testBeaconBlock = beaconBlock;

    assertEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    BeaconBlock testBeaconBlock =
        new BeaconBlock(slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);

    assertEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenSlotsAreDifferent() {
    BeaconBlock testBeaconBlock =
        new BeaconBlock(
            slot + randomLong(), parentRoot, stateRoot, randaoReveal, eth1Data, signature, body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenAncestorHashesAreDifferent() {
    BeaconBlock testBeaconBlock =
        new BeaconBlock(slot, parentRoot.not(), stateRoot, randaoReveal, eth1Data, signature, body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenStateRootsAreDifferent() {
    BeaconBlock testBeaconBlock =
        new BeaconBlock(slot, parentRoot, stateRoot.not(), randaoReveal, eth1Data, signature, body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenRandaoRevealsAreDifferent() {
    // Create copy of randaoReveal and reverse to ensure it is different.
    List<Bytes48> reverseRandaoReveal = new ArrayList<Bytes48>(randaoReveal);
    Collections.reverse(reverseRandaoReveal);

    BeaconBlock testBeaconBlock =
        new BeaconBlock(
            slot, parentRoot, stateRoot, reverseRandaoReveal, eth1Data, signature, body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenEth1DataIsDifferent() {
    BeaconBlock testBeaconBlock =
        new BeaconBlock(
            slot,
            parentRoot,
            stateRoot,
            randaoReveal,
            new Eth1Data(eth1Data.getDeposit_root().not(), eth1Data.getBlock_hash().not()),
            signature,
            body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenSignaturesAreDifferent() {
    // Create copy of signature and reverse to ensure it is different.
    BLSSignature reverseSignature = new BLSSignature(signature.getC1(), signature.getC0());

    BeaconBlock testBeaconBlock =
        new BeaconBlock(
            slot, parentRoot, stateRoot, randaoReveal, eth1Data, reverseSignature, body);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void equalsReturnsFalseWhenBeaconBlockBodiesAreDifferent() {
    // BeaconBlock is rather involved to create. Just create a random one until it is not the same
    // as the original.
    BeaconBlockBody otherBody = randomBeaconBlockBody();
    while (Objects.equals(otherBody, body)) {
      otherBody = randomBeaconBlockBody();
    }

    BeaconBlock testBeaconBlock =
        new BeaconBlock(slot, parentRoot, stateRoot, randaoReveal, eth1Data, signature, otherBody);

    assertNotEquals(beaconBlock, testBeaconBlock);
  }

  @Test
  void rountripSSZ() {
    Bytes sszBeaconBlockBytes = beaconBlock.toBytes();
    assertEquals(beaconBlock, BeaconBlock.fromBytes(sszBeaconBlockBytes));
  }
}