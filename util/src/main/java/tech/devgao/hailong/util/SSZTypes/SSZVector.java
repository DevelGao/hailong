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

package tech.devgao.hailong.util.SSZTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SSZVector<T> extends ArrayList<T> {

  private int maxSize;
  private Class<T> classInfo;

  public SSZVector() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("SSZVector must have specified size");
  }

  @SuppressWarnings("unchecked")
  public SSZVector(int size, T object) {
    super(Collections.nCopies(size, object));
    this.maxSize = size;
    classInfo = (Class<T>) object.getClass();
  }

  public SSZVector(List<T> list, Class<T> classInfo) {
    super(list);
    maxSize = list.size();
    this.classInfo = classInfo;
  }

  public SSZVector(SSZVector<T> list) {
    super(list);
    maxSize = list.size();
    this.classInfo = list.getElementType();
  }

  public int getSize() {
    return maxSize;
  }

  @Override
  public boolean add(T object) {
    throw new UnsupportedOperationException("SSZVector does not support add, only set");
  }

  public Class<T> getElementType() {
    return classInfo;
  }
}
