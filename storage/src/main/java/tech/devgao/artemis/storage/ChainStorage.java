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

package tech.devgao.artemis.storage;

import com.google.common.eventbus.EventBus;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Level;
import tech.devgao.artemis.util.alogger.ALogger;

/** ChainStorage Interface class */
public interface ChainStorage {

  ALogger LOG = new ALogger(ChainStorage.class.getName());

  /**
   * Instantiate the ChainStorage
   *
   * @param type
   * @param eventBus
   * @return
   */
  static <T> T Create(Class<T> type, EventBus eventBus) {
    try {
      return type.getDeclaredConstructor(EventBus.class).newInstance(eventBus);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add item to Queue
   *
   * @param item
   * @param items
   */
  static <S, T extends Queue<S>> void add(S item, T items) {
    try {
      items.add(item);
    } catch (IllegalStateException e) {
      LOG.log(Level.DEBUG, items.getClass().toString() + ": " + e.getMessage());
    }
  }

  /**
   * Add a value to a HashMap
   *
   * @param key
   * @param value
   * @param items
   */
  static <S, T, U extends ConcurrentHashMap<S, T>> void add(S key, T value, U items) {
    try {
      items.put(key, value);
    } catch (IllegalStateException e) {
      LOG.log(Level.DEBUG, items.getClass().toString() + ": " + e.getMessage(), true);
    }
  }

  /**
   * Retrieve a value from a HashMap
   *
   * @param key
   * @param items
   * @return
   */
  static <S, T, U extends ConcurrentHashMap<S, T>> Optional<T> get(S key, U items) {
    Optional<T> result = Optional.empty();
    try {
      if (items.size() > 0) {
        result = Optional.of(items.get(key));
      }
    } catch (NullPointerException e) {
      if (!key.toString()
          .equalsIgnoreCase("0x0000000000000000000000000000000000000000000000000000000000000000")) {
        LOG.log(Level.DEBUG, items.getClass().toString() + ": " + key.toString() + " not found.");
      }
    }
    return result;
  }

  /**
   * Delete a value from a HashMap
   *
   * @param key
   * @param items
   * @return
   */
  static <S, T, U extends ConcurrentHashMap<S, T>> boolean remove(S key, U items) {
    try {
      items.remove(key);
    } catch (NullPointerException e) {
      if (!key.toString()
          .equalsIgnoreCase("0x0000000000000000000000000000000000000000000000000000000000000000")) {
        LOG.log(Level.DEBUG, items.getClass().toString() + ": " + key.toString() + " not found.");
        return false;
      }
    }
    return true;
  }

  /**
   * Remove an item from a Queue
   *
   * @param items
   * @return
   */
  static <S, T extends Queue<S>> Optional<S> remove(T items) {
    Optional<S> result = Optional.empty();
    if (items.size() > 0) {
      result = Optional.of(items.poll());
    }
    return result;
  }

  /**
   * Peek an item from a Queue
   *
   * @param items
   * @return
   */
  static <S, T extends Queue<S>> Optional<S> peek(T items) {
    Optional<S> result = Optional.empty();
    if (items.size() > 0) {
      result = Optional.of(items.peek());
    }
    return result;
  }
}
