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

package net.develgao.artemis.util.bytes;

/**
 * An implementation of {@link MutableBytes3} backed by a byte array ({@code byte[]}).
 */
class MutableArrayWrappingBytes3 extends MutableArrayWrappingBytesValue implements MutableBytes3 {

  MutableArrayWrappingBytes3(byte[] bytes) {
    this(bytes, 0);
  }

  MutableArrayWrappingBytes3(byte[] bytes, int offset) {
    super(bytes, offset, SIZE);
  }

  @Override
  public Bytes3 copy() {
    // We *must* override this method because ArrayWrappingBytes3 assumes that it is the case.
    return new ArrayWrappingBytes3(arrayCopy());
  }

  @Override
  public MutableBytes3 mutableCopy() {
    return new MutableArrayWrappingBytes3(arrayCopy());
  }
}
