/*
 * Copyright 2020 Developer Gao.
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

package tech.devgao.hailong.benchmarks.gen;

import static org.mockito.Mockito.mock;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_committee_count_at_slot;
import static tech.devgao.hailong.datastructures.util.CommitteeUtil.get_beacon_committee;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.benchmarks.gen.BlockIO.Writer;
import tech.devgao.hailong.data.BlockProcessingRecord;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.util.BeaconStateUtil;
import tech.devgao.hailong.statetransition.AttestationGenerator;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.config.Constants;

/**
 * Utility class for generating BLS keypairs and blocks files Test methods need to be run manually
 */
public class Generator {

  @Disabled
  @Test
  public void generateBlocks() throws Exception {

    Constants.SLOTS_PER_EPOCH = 6;

    BeaconStateUtil.BLS_VERIFY_DEPOSIT = false;
    BeaconStateUtil.DEPOSIT_PROOFS_ENABLED = false;

    System.out.println("Generating keypairs...");
    int validatorsCount = 10 * 1024;

    List<BLSKeyPair> validatorKeys =
        BlsKeyPairIO.createReaderForResource("/bls-key-pairs/bls-key-pairs-100k-seed-0.txt.gz")
            .readAll(validatorsCount);

    System.out.println("Keypairs done.");

    EventBus localEventBus = mock(EventBus.class);
    ChainStorageClient localStorage = ChainStorageClient.memoryOnlyClient(localEventBus);
    BeaconChainUtil localChain = BeaconChainUtil.create(localStorage, validatorKeys, false);
    localChain.initializeStorage();
    AttestationGenerator attestationGenerator = new AttestationGenerator(validatorKeys);

    UnsignedLong currentSlot = localStorage.getBestSlot();
    List<Attestation> attestations = Collections.emptyList();

    String blocksFile =
        "blocks_epoch_" + Constants.SLOTS_PER_EPOCH + "_validators_" + validatorsCount + ".ssz";

    try (Writer writer = BlockIO.createFileWriter(blocksFile)) {

      for (int j = 0; j < 50; j++) {
        for (int i = 0; i < Constants.SLOTS_PER_EPOCH; i++) {
          long s = System.currentTimeMillis();
          currentSlot = currentSlot.plus(UnsignedLong.ONE);
          //        Optional<BeaconState> lastState =
          // localStorage.getBlockState(localStorage.getBestBlockRoot());
          //        Optional<BeaconBlock> lastBlock =
          // localStorage.getBlockByRoot(localStorage.getBestBlockRoot());
          BlockProcessingRecord record =
              localChain.createAndImportBlockAtSlot(
                  currentSlot, AttestationGenerator.groupAndAggregateAttestations(attestations));

          final SignedBeaconBlock block = record.getBlock();
          writer.accept(block);

          attestations =
              UnsignedLong.ONE.equals(currentSlot)
                  ? Collections.emptyList()
                  : attestationGenerator.getAttestationsForSlot(
                      record.getPostState(), block.getMessage(), currentSlot);

          System.out.println(
              "Processed: "
                  + currentSlot
                  + ", "
                  + getCommittees(record.getPostState())
                  + ", "
                  + (System.currentTimeMillis() - s)
                  + " ms");
        }

        Optional<BeaconState> bestState =
            localStorage.getBlockState(localStorage.getBestBlockRoot());
        System.out.println("Epoch done: " + bestState);
      }
    }
  }

  @Disabled
  @Test
  public void generateKeyPairs() throws Exception {
    int randomSeed = 0;
    int limitK = 100;
    File outFile = new File("bls-key-pairs-" + limitK + "k-seed-" + randomSeed + ".txt");
    Iterator<BLSKeyPair> keyPairIterator =
        IntStream.range(randomSeed, randomSeed + Integer.MAX_VALUE)
            .mapToObj(BLSKeyPair::random)
            .iterator();

    System.out.println("Generating keypairs...");
    try (BlsKeyPairIO.Writer writer = BlsKeyPairIO.createWriter(outFile, keyPairIterator::next)) {
      for (int i = 0; i < limitK; i++) {
        writer.write(1024);
        System.out.println("Generated " + (i + 1) + "K");
      }
    }

    // check
    try (BlsKeyPairIO.Reader reader = BlsKeyPairIO.createReaderForFile(outFile.getName())) {
      for (BLSKeyPair keyPair : reader.withLimit(10)) {
        keyPair.getPublicKey().hashCode();
        System.out.println(keyPair);
      }
    }
  }

  String getCommittees(BeaconState state) {
    UnsignedLong cnt = get_committee_count_at_slot(state, state.getSlot());
    List<List<Integer>> committees = new ArrayList<>();
    for (UnsignedLong index = UnsignedLong.ZERO;
        index.compareTo(cnt) < 0;
        index = index.plus(UnsignedLong.ONE)) {

      committees.add(get_beacon_committee(state, state.getSlot(), index));
    }

    return "["
        + committees.stream()
            .map(com -> com.stream().map(i -> "" + i).collect(Collectors.joining(",")))
            .collect(Collectors.joining("],["))
        + "]";
  }
}
