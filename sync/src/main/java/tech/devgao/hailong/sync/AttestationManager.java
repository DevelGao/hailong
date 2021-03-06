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

package tech.devgao.hailong.sync;

import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.operations.AggregateAndProof;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.service.serviceutils.Service;
import tech.devgao.hailong.statetransition.StateTransition;
import tech.devgao.hailong.statetransition.attestation.AttestationProcessingResult;
import tech.devgao.hailong.statetransition.attestation.ForkChoiceAttestationProcessor;
import tech.devgao.hailong.statetransition.events.BlockImportedEvent;
import tech.devgao.hailong.statetransition.events.ProcessedAggregateEvent;
import tech.devgao.hailong.statetransition.events.ProcessedAttestationEvent;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.events.SlotEvent;
import tech.devgao.hailong.util.async.SafeFuture;

public class AttestationManager extends Service {
  private static final Logger LOG = LogManager.getLogger();
  private final EventBus eventBus;
  private final ForkChoiceAttestationProcessor attestationProcessor;
  private final PendingPool<DelayableAttestation> pendingAttestations;
  private final FutureItems<DelayableAttestation> futureAttestations;

  AttestationManager(
      final EventBus eventBus,
      final ForkChoiceAttestationProcessor attestationProcessor,
      final PendingPool<DelayableAttestation> pendingAttestations,
      final FutureItems<DelayableAttestation> futureAttestations) {
    this.eventBus = eventBus;
    this.attestationProcessor = attestationProcessor;
    this.pendingAttestations = pendingAttestations;
    this.futureAttestations = futureAttestations;
  }

  public static AttestationManager create(
      final EventBus eventBus, final ChainStorageClient storageClient) {
    final PendingPool<DelayableAttestation> pendingAttestations =
        PendingPool.createForAttestations(eventBus);
    final FutureItems<DelayableAttestation> futureAttestations =
        new FutureItems<>(DelayableAttestation::getEarliestSlotForProcessing);
    return new AttestationManager(
        eventBus,
        new ForkChoiceAttestationProcessor(storageClient, new StateTransition(false)),
        pendingAttestations,
        futureAttestations);
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onGossipedAttestation(final Attestation attestation) {
    processAttestation(
        new DelayableAttestation(
            attestation, () -> eventBus.post(new ProcessedAttestationEvent(attestation))));
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onAggregateAndProof(final AggregateAndProof aggregateAndProof) {
    final Attestation aggregate = aggregateAndProof.getAggregate();
    processAttestation(
        new DelayableAttestation(
            aggregate, () -> eventBus.post(new ProcessedAggregateEvent(aggregate))));
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onSlot(final SlotEvent slotEvent) {
    futureAttestations.prune(slotEvent.getSlot()).forEach(this::processAttestation);
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onBlockImported(final BlockImportedEvent blockImportedEvent) {
    final SignedBeaconBlock block = blockImportedEvent.getBlock();
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    pendingAttestations
        .getItemsDependingOn(blockRoot, false)
        .forEach(
            attestation -> {
              pendingAttestations.remove(attestation);
              processAttestation(attestation);
            });
  }

  private void processAttestation(final DelayableAttestation delayableAttestation) {
    if (pendingAttestations.contains(delayableAttestation)) {
      return;
    }
    final AttestationProcessingResult result =
        attestationProcessor.processAttestation(delayableAttestation.getAttestation());
    if (result.isSuccessful()) {
      LOG.trace("Processed attestation {} successfully", delayableAttestation::hash_tree_root);
      delayableAttestation.onAttestationProcessedSuccessfully();
    } else {
      switch (result.getFailureReason()) {
        case UNKNOWN_BLOCK:
          LOG.trace(
              "Deferring attestation {} as require block is not yet present",
              delayableAttestation::hash_tree_root);
          pendingAttestations.add(delayableAttestation);
          break;
        case ATTESTATION_IS_NOT_FROM_PREVIOUS_SLOT:
        case FOR_FUTURE_EPOCH:
          LOG.trace(
              "Deferring attestation {} until a future slot", delayableAttestation::hash_tree_root);
          futureAttestations.add(delayableAttestation);
          break;
        default:
          STDOUT.log(Level.WARN, "Failed to process attestation: " + result.getFailureMessage());
          break;
      }
    }
  }

  @Override
  protected SafeFuture<?> doStart() {
    eventBus.register(this);
    return this.pendingAttestations.start();
  }

  @Override
  protected SafeFuture<?> doStop() {
    eventBus.unregister(this);
    return pendingAttestations.stop();
  }
}
