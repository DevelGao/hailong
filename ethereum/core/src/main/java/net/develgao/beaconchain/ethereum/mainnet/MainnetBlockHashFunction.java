package net.develgao.beaconchain.ethereum.mainnet;

import net.develgao.beaconchain.ethereum.core.BlockHeader;
import net.develgao.beaconchain.ethereum.core.Hash;
import net.develgao.beaconchain.ethereum.rlp.RLP;
import net.develgao.beaconchain.util.bytes.BytesValue;

/**
 * Implements the block hashing algorithm for MainNet as per the yellow paper.
 */
public class MainnetBlockHashFunction {

  public static Hash createHash(BlockHeader header) {
    BytesValue rlp = RLP.encode(header::writeTo);
    return Hash.hash(rlp);
  }
}
