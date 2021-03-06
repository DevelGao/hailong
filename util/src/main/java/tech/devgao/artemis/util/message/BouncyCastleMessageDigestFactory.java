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

package tech.devgao.artemis.util.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BouncyCastleMessageDigestFactory {

  private static final BouncyCastleProvider securityProvider = new BouncyCastleProvider();

  @SuppressWarnings("DoNotInvokeMessageDigestDirectly")
  public static MessageDigest create(String algorithm) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(algorithm, securityProvider);
  }
}
