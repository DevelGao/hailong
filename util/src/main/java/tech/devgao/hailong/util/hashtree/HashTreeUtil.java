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

package tech.devgao.hailong.util.hashtree;

import static java.lang.Long.max;
import static java.lang.Math.toIntExact;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.hailong.util.SSZTypes.Bitlist;
import tech.devgao.hailong.util.SSZTypes.Bitvector;
import tech.devgao.hailong.util.SSZTypes.SSZVector;
import tech.devgao.hailong.util.bls.BLSPublicKey;

/** This class is a collection of tree hash root convenience methods */
public final class HashTreeUtil {

  /**
   * A enum defining different SSZ types. See
   * https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md.
   *
   * <p>Basic Types: - BASIC: A uint# or a byte (uint8) Composite Types: - CONTAINER: A collection
   * of arbitrary other SSZ types. - VECTOR: A fixed-length collection of homogenous values. -
   * _OF_BASIC: A vector containing only basic SSZ types. - _OF_COMPOSITE: A vector containing
   * homogenous SSZ composite types. - LIST: A variable-length collection of homogenous values. -
   * _OF_BASIC: A list containing only basic SSZ types. - _OF_COMPOSITE: A list containing
   * homogenous SSZ composite types.
   */
  public enum SSZTypes {
    BASIC,
    CONTAINER,
    VECTOR_OF_BASIC,
    VECTOR_OF_COMPOSITE,
    LIST_OF_BASIC,
    LIST_OF_COMPOSITE,
    BITLIST,
    BITVECTOR
  };

  private static List<Bytes32> zerohashes = getZerohashes();

  private static List<Bytes32> getZerohashes() {
    List<Bytes32> zerohashes = new ArrayList<>();
    zerohashes.add(Bytes32.ZERO);
    IntStream.range(1, 100)
        .forEach(
            i ->
                zerohashes.add(
                    Hash.sha2_256(
                        Bytes.concatenate(zerohashes.get(i - 1), zerohashes.get(i - 1)))));
    return zerohashes;
  }

  // BYTES_PER_CHUNK is rather tightly coupled to the value 32 due to the assumption that it fits in
  // the Byte32 type. Use care if this ever has to change.
  public static final int BYTES_PER_CHUNK = 32;

  public static Bytes32 hash_tree_root(SSZTypes sszType, Bytes... bytes) {
    switch (sszType) {
      case BASIC:
        return hash_tree_root_basic_type(bytes);
      case BITLIST:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZType.BITLIST, int, Bytes...) for a bitlist type.");
      case VECTOR_OF_BASIC:
        return hash_tree_root_vector_of_basic_type(bytes);
      case VECTOR_OF_COMPOSITE:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZTypes.TUPLE_OF_COMPOSITE, List) for a fixed length list of composite SSZ types.");
      case LIST_OF_BASIC:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZType.LIST_OF_BASIC, int, Bytes...) for a variable length list of basic SSZ type.");
      case LIST_OF_COMPOSITE:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZTypes.LIST_COMPOSITE, List) for a variable length list of composite SSZ types.");
      case CONTAINER:
        throw new UnsupportedOperationException(
            "hash_tree_root of SSZ Containers (often implemented by POJOs) must be done by the container POJO itself, as its individual fields cannot be enumerated without reflection.");
      default:
        break;
    }
    return Bytes32.ZERO;
  }

  /*
  public static Bytes32 hash_tree_root(SSZTypes sszType, long maxSize, Bytes... bytes) {
    switch (sszType) {
      case BITLIST:
        checkArgument(bytes.length == 1, "A BitList is only represented by a single Bytes value");
        return hash_tree_root_bitlist(bytes[0], maxSize);
      default:
        throw new UnsupportedOperationException(
            "The maxSize parameter is only applicable for SSZ Lists.");
    }
  }
  */

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Bytes32 hash_tree_root(SSZTypes sszType, List bytes) {
    switch (sszType) {
      case LIST_OF_BASIC:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZTypes.LIST_OF_BASIC, int, List) for a variable length list of basic SSZ types.");
      case LIST_OF_COMPOSITE:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZTypes.LIST_OF_COMPOSITE, int, List) for a variable length list of composite SSZ types.");
      case VECTOR_OF_COMPOSITE:
        if (!bytes.isEmpty() && bytes.get(0) instanceof Bytes32) {
          return hash_tree_root_vector_composite_type((List<Bytes32>) bytes);
        }
        break;
      case BASIC:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZType.BASIC, Bytes...) for a basic SSZ type.");
      case VECTOR_OF_BASIC:
        throw new UnsupportedOperationException(
            "Use HashTreeUtil.hash_tree_root(SSZTypes.TUPLE_BASIC, Bytes...) for a fixed length tuple of basic SSZ types.");
      case CONTAINER:
        throw new UnsupportedOperationException(
            "hash_tree_root of SSZ Containers (often implemented by POJOs) must be done by the container POJO itself, as its individual fields cannot be enumerated without reflection.");
      default:
    }
    return Bytes32.ZERO;
  }

  public static Bytes32 hash_tree_root_list_ul(long maxSize, List<Bytes> bytes) {
    return hash_tree_root_list_of_unsigned_long(bytes, maxSize, bytes.size());
  }

  public static Bytes32 hash_tree_root_list_bytes(long maxSize, List<Bytes32> bytes) {
    return hash_tree_root_list_bytes(bytes, maxSize, bytes.size());
  }

  public static Bytes32 hash_tree_root_vector_unsigned_long(SSZVector<UnsignedLong> vector) {
    List<Bytes> bytes =
        vector.stream().map(i -> SSZ.encodeUInt64(i.longValue())).collect(Collectors.toList());
    return merkleize(pack(bytes.toArray(new Bytes[0])));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Bytes32 hash_tree_root(SSZTypes sszType, long maxSize, List bytes) {
    switch (sszType) {
      case LIST_OF_COMPOSITE:
        return hash_tree_root_list_composite_type(
            (List<Merkleizable>) bytes, maxSize, bytes.size());
      default:
        throw new UnsupportedOperationException(
            "The maxSize parameter is only applicable for SSZ Lists.");
    }
  }

  public static Bytes32 merkleize1(List<Bytes32> sszChunks, long limit) {
    List<Bytes32> mutableSSZChunks = new ArrayList<>(sszChunks);

    // A balanced binary tree must have a power of two number of leaves.
    // NOTE: is_power_of_two also serves as a zero size check here.
    while (!is_power_of_two(mutableSSZChunks.size()) || mutableSSZChunks.size() < limit) {
      mutableSSZChunks.add(Bytes32.ZERO);
    }

    // Expand the size of the mutableSSZChunks list large enough to hold the entire tree.
    mutableSSZChunks.addAll(0, Collections.nCopies(mutableSSZChunks.size(), Bytes32.ZERO));

    // Iteratively calculate the root for each parent in the binary tree, starting at the leaves.
    for (int inlineTreeIndex = mutableSSZChunks.size() / 2 - 1;
        inlineTreeIndex > 0;
        inlineTreeIndex--) {
      mutableSSZChunks.set(
          inlineTreeIndex,
          Hash.sha2_256(
              Bytes.concatenate(
                  mutableSSZChunks.get(inlineTreeIndex * 2),
                  mutableSSZChunks.get(inlineTreeIndex * 2 + 1))));
    }

    // Return the root element, which is at index 1. The math is easier this way.
    return mutableSSZChunks.get(1);
  }

  public static Bytes32 merkleize(List<Bytes32> chunks, long limit) {
    int count = chunks.size();
    if (limit == 0) return zerohashes.get(0);

    int depth = Long.SIZE - Long.numberOfLeadingZeros(max(count - 1, 0));
    int max_depth = Long.SIZE - Long.numberOfLeadingZeros(limit - 1);
    List<Bytes32> tmp = new ArrayList<>();
    IntStream.range(0, max_depth + 1).boxed().forEach(i -> tmp.add(null));

    IntStream.range(0, count).forEach(i -> merge(chunks.get(i), i, tmp, count, depth));

    if ((1 << depth) != count) {
      merge(zerohashes.get(0), count, tmp, count, depth);
    }

    IntStream.range(depth, max_depth)
        .forEach(
            j -> tmp.set(j + 1, Hash.sha2_256(Bytes.concatenate(tmp.get(j), zerohashes.get(j)))));

    return tmp.get(max_depth);
  }

  private static void merge(Bytes32 h, int i, List<Bytes32> tmp, long count, int depth) {
    int j = 0;
    while (true) {
      if ((i & (1 << j)) == 0) {
        if (i == count && j < depth) {
          h = Hash.sha2_256(Bytes.concatenate(h, zerohashes.get(j)));
        } else {
          break;
        }
      } else {
        h = Hash.sha2_256(Bytes.concatenate(tmp.get(j), h));
      }
      j += 1;
    }
    tmp.set(j, h);
  }

  public static Bytes32 merkleize(List<Bytes32> sszChunks) {
    return merkleize(sszChunks, sszChunks.size());
  }

  /**
   * Create the hash tree root of a set of values of basic SSZ types or tuples of basic types. Basic
   * SSZ types are uintN, bool, and byte. bytesN (i.e. Bytes32) is a tuple of basic types. NOTE:
   * Bytes (and not Bytes32, Bytes48 etc.) IS NOT a basic type or a tuple of basic types.
   *
   * @param bytes One Bytes value or a list of homogeneous Bytes values.
   * @return The SSZ tree root hash of the values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_basic_type(Bytes... bytes) {
    return merkleize(pack(bytes));
  }

  /**
   * Create the hash tree root of a set of values of basic SSZ types or tuples of basic types. Basic
   * SSZ types are uintN, bool, and byte. bytesN (i.e. Bytes32) is a tuple of basic types. NOTE:
   * Bytes (and not Bytes32, Bytes48 etc.) IS NOT a basic type or a tuple of basic types.
   *
   * @param bytes One Bytes value or a list of homogeneous Bytes values.
   * @return The SSZ tree root hash of the values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_vector_of_basic_type(Bytes... bytes) {
    return hash_tree_root_basic_type(bytes);
  }

  /**
   * Create the hash tree root of a SSZ Bitlist.
   *
   * @return The SSZ tree root hash of the values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  public static Bytes32 hash_tree_root_bitlist(Bitlist bitlist) {
    // TODO The following lines are a hack and can be fixed once we shift from Bytes to a real
    // bitlist type.
    return mix_in_length(
        merkleize(
            bitfield_bytes(bitlist.serialize()),
            chunk_count(SSZTypes.BITLIST, bitlist.getMaxSize())),
        bitlist.getByteArray().length);
  }

  /**
   * Create the hash tree root of a SSZ Bitvector.
   *
   * @param bitvector One Bytes value or a list of homogeneous Bytes values.
   * @return The SSZ tree root hash of the values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  public static Bytes32 hash_tree_root_bitvector(Bitvector bitvector) {
    return merkleize(
        pack(bitvector.serialize()), chunk_count(SSZTypes.BITVECTOR, bitvector.getSize()));
  }

  /**
   * Create the hash tree root of a list of values of basic SSZ types. This is only to be used for
   * SSZ lists and not SSZ tuples. See the "see also" for more info.
   *
   * @param bytes A list of homogeneous Bytes values representing basic SSZ types.
   * @return The SSZ tree root hash of the list of values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_list_of_unsigned_long(
      List<? extends Bytes> bytes, long maxSize, int length) {
    return mix_in_length(
        merkleize(pack(bytes.toArray(new Bytes[0])), chunk_count_list_unsigned_long(maxSize)),
        length);
  }

  /**
   * Create the hash tree root of a list of values of basic SSZ types. This is only to be used for
   * SSZ lists and not SSZ tuples. See the "see also" for more info.
   *
   * @param bytes A list of homogeneous Bytes values representing basic SSZ types.
   * @return The SSZ tree root hash of the list of values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_list_bytes(List<Bytes32> bytes, long maxSize, int length) {
    return mix_in_length(
        merkleize(bytes, chunk_count(SSZTypes.LIST_OF_COMPOSITE, maxSize)), length);
  }

  public static Bytes32 hash_tree_root_list_pubkey(List<BLSPublicKey> pubkeys, long maxSize) {
    return hash_tree_root_list_pubkey(pubkeys, maxSize, pubkeys.size());
  }

  private static Bytes32 hash_tree_root_list_pubkey(
      List<BLSPublicKey> bytes, long maxSize, int length) {
    List<Bytes32> hashTreeRootList =
        bytes.stream()
            .map(item -> hash_tree_root(SSZTypes.VECTOR_OF_BASIC, item.toBytes()))
            .collect(Collectors.toList());
    return mix_in_length(
        merkleize(hashTreeRootList, chunk_count(SSZTypes.LIST_OF_COMPOSITE, maxSize)), length);
  }

  /**
   * Create the hash tree root of a list of values of basic SSZ types. This is only to be used for
   * SSZ lists and not SSZ tuples. See the "see also" for more info.
   *
   * @param bytes A list of homogeneous Bytes values representing basic SSZ types.
   * @return The SSZ tree root hash of the list of values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_list_composite_type(
      List<? extends Merkleizable> bytes, long maxSize, int length) {
    List<Bytes32> hashTreeRootList =
        bytes.stream().map(item -> item.hash_tree_root()).collect(Collectors.toList());
    return mix_in_length(
        merkleize(hashTreeRootList, chunk_count(SSZTypes.LIST_OF_COMPOSITE, maxSize)), length);
  }

  /**
   * Create the hash tree root of a tuple of composite SSZ types. This is only to be used for SSZ
   * tuples and not SSZ lists. See the "see also" for more info. NOTE: This function assumes the
   * composite type is a tuple of basic types.
   *
   * @param bytes A list of homogeneous Bytes32 values.
   * @return The SSZ tree root hash of the list of values.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/simple-serialize.md">SSZ
   *     Spec v0.5.1</a>
   */
  private static Bytes32 hash_tree_root_vector_composite_type(List<Bytes32> bytes) {
    return merkleize(
        bytes.stream()
            .map(item -> hash_tree_root_vector_of_basic_type(item))
            .collect(Collectors.toList()));
  }

  private static List<Bytes32> bitfield_bytes(Bytes sszValues) {
    // Reverse byte order to big endian.
    Bytes reversedBytes = sszValues.reverse();
    int shiftCount = 8 - ((reversedBytes.bitLength() - 1) % 8);
    // Left shift to remove leading one bit-marker when serializing bitlists.
    Bytes truncatedBitfield = reversedBytes.shiftLeft(shiftCount);
    // Right shift to return list back to normal (excluding marker bit).
    Bytes resultantBitfield = truncatedBitfield.shiftRight(shiftCount);
    // If removing marker bit allows bitfield to be packed in less bytes, trim as necessary.
    Bytes trimmedBitfield = resultantBitfield.trimLeadingZeros();
    // Turn bytes back into little endian, and pack.
    return pack(trimmedBitfield.reverse());
  }

  private static List<Bytes32> pack(Bytes... sszValues) {
    // Join all varags sszValues into one Bytes type
    Bytes concatenatedBytes = Bytes.concatenate(sszValues);

    // Pad so that concatenatedBytes length is divisible by BYTES_PER_CHUNK
    int packingRemainder = concatenatedBytes.size() % BYTES_PER_CHUNK;
    if (packingRemainder != 0) {
      concatenatedBytes =
          Bytes.concatenate(
              concatenatedBytes, Bytes.wrap(new byte[BYTES_PER_CHUNK - packingRemainder]));
    }

    // Wrap each BYTES_PER_CHUNK-byte value into a Bytes32
    List<Bytes32> chunkifiedBytes = new ArrayList<>();
    for (int chunk = 0; chunk < concatenatedBytes.size(); chunk += BYTES_PER_CHUNK) {
      chunkifiedBytes.add(Bytes32.wrap(concatenatedBytes, chunk));
    }

    return chunkifiedBytes;
  }

  private static long chunk_count_list_unsigned_long(long maxSize) {
    return (maxSize * 8 + 31) / 32;
  }

  private static long chunk_count(HashTreeUtil.SSZTypes sszType, long maxSize) {
    switch (sszType) {
      case BASIC:
        throw new UnsupportedOperationException(
            "Use chunk_count(HashTreeUtil.SSZTypes, Bytes) for BASIC SSZ types.");
      case BITLIST:
        // TODO The following lines are a hack and can be fixed once we shift from Bytes to a real
        // bitlist type.
        long chunkCount = (maxSize + 255) / 256;
        return chunkCount > 0 ? chunkCount : 1;
      case BITVECTOR:
        return (maxSize + 255) / 256;
      case LIST_OF_BASIC:
        throw new UnsupportedOperationException(
            "Use chunk_count_list_unsigned_long(int, Bytes) for List of uint64 SSZ types.");
      case VECTOR_OF_BASIC:
        throw new UnsupportedOperationException(
            "Use chunk_count(HashTreeUtil.SSZTypes, Bytes) for VECTORS of BASIC SSZ types.");
      case LIST_OF_COMPOSITE:
        return maxSize;
      case VECTOR_OF_COMPOSITE:
        throw new UnsupportedOperationException(
            "Use chunk_count(HashTreeUtil.SSZTypes, Bytes) for VECTORS of BASIC SSZ types.");
      case CONTAINER:
        throw new UnsupportedOperationException(
            "hash_tree_root of SSZ Containers (often implemented by POJOs) must be done by the container POJO itself, as its individual fields cannot be enumerated without reflection.");
    }
    return -1;
  }

  private static Bytes32 mix_in_length(Bytes32 merkle_root, int length) {
    // Append the little-endian length mixin to the given merkle root, and return its hash.
    return Hash.sha2_256(
        Bytes.concatenate(
            merkle_root, Bytes.ofUnsignedLong(length, LITTLE_ENDIAN), Bytes.wrap(new byte[24])));
  }

  public static boolean is_power_of_two(int value) {
    return value > 0 && (value & (value - 1)) == 0;
  }

  public static boolean is_power_of_two(UnsignedLong value) {
    return is_power_of_two(toIntExact(value.longValue()));
  }
}
