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

package tech.devgao.hailong.events;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import tech.devgao.hailong.util.events.Subscribers;

abstract class EventDeliverer<T> implements InvocationHandler {
  private final Subscribers<T> subscribers = Subscribers.create(true);

  void subscribe(T subscriber) {
    subscribers.subscribe(subscriber);
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) {
    if (method.getDeclaringClass().equals(Object.class)) {
      try {
        return method.invoke(this, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
    subscribers.forEach(subscriber -> deliverTo(subscriber, method, args));
    return null;
  }

  protected abstract void deliverTo(T subscriber, Method method, Object[] args);

  public void stop() {}
}
