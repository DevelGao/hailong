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

package tech.devgao.artemis.state.util;

import static java.lang.Math.toIntExact;
import static tech.devgao.artemis.Constants.LATEST_BLOCK_ROOTS_LENGTH;
import static tech.devgao.artemis.Constants.LATEST_RANDAO_MIXES_LENGTH;

import tech.devgao.artemis.datastructures.beaconchainblocks.BeaconBlock;
import tech.devgao.artemis.datastructures.beaconchainoperations.LatestBlockRoots;
import tech.devgao.artemis.datastructures.beaconchainstate.ValidatorRecord;
import tech.devgao.artemis.datastructures.beaconchainstate.Validators;
import tech.devgao.artemis.ethereum.core.Hash;
import tech.devgao.artemis.services.beaconchain.StateTransitionException;
import tech.devgao.artemis.state.BeaconState;
import tech.devgao.artemis.util.bytes.Bytes32;
import tech.devgao.artemis.util.uint.UInt64;

import java.util.ArrayList;

public class SlotProcessorUtil {

    public static Hash merkle_root(LatestBlockRoots latest_block_roots){
        //todo
        return Hash.wrap(Bytes32.FALSE);
    }

    public static void updateProposerRandaoLayer(BeaconState state){
        int currSlot = toIntExact(state.getSlot());
        int proposerIndex = BeaconState.get_beacon_proposer_index(state, currSlot);

        Validators validators = state.getValidator_registry();
        ValidatorRecord proposerRecord = validators.get(proposerIndex);
        proposerRecord.setRandao_layers(proposerRecord.getRandao_layers().increment());
    }

    public static void updateLatestRandaoMixes(BeaconState state){
        int currSlot = toIntExact(state.getSlot());
        ArrayList<Hash> latestRandaoMixes = state.getLatest_randao_mixes();
        Hash prevSlotRandaoMix = latestRandaoMixes.get((currSlot - 1) % LATEST_RANDAO_MIXES_LENGTH);
        latestRandaoMixes.set(currSlot % LATEST_RANDAO_MIXES_LENGTH, prevSlotRandaoMix);
    }

    public static void updateRecentBlockHashes(BeaconState state, BeaconBlock block) throws StateTransitionException {
        Hash previous_state_root = block.getState_root();
        if(previous_state_root!=null) state.getLatest_block_roots().put(UInt64.valueOf(state.getSlot()), previous_state_root);
        else throw new StateTransitionException("StateTransitionException: BeaconState cannot be updated due to previous_state_root returning a null");

        if(state.getSlot() % LATEST_BLOCK_ROOTS_LENGTH == 0){
            ArrayList<Hash> batched_block_roots = state.getBatched_block_roots();
            LatestBlockRoots latest_block_roots = state.getLatest_block_roots();
            if(batched_block_roots != null && latest_block_roots != null){
                Hash merkle_root = SlotProcessorUtil.merkle_root(latest_block_roots);
                batched_block_roots.add(merkle_root);
            }
            else throw new StateTransitionException("StateTransitionException: BeaconState cannot be updated due to batched_block_roots and latest_block_roots returning a null");
        }
    }
}
