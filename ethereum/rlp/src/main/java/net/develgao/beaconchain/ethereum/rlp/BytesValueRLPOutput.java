package net.develgao.beaconchain.ethereum.rlp;

import net.develgao.beaconchain.util.bytes.BytesValue;
import net.develgao.beaconchain.util.bytes.MutableBytesValue;


/**
 * An {@link RLPOutput} that writes RLP encoded data to a {@link BytesValue}.
 */
public class BytesValueRLPOutput extends AbstractRLPOutput {
  /**
   * Computes the final encoded data.
   *
   * @return A value containing the data written to this output RLP-encoded.
   */
  public BytesValue encoded() {
    int size = encodedSize();
    if (size == 0) {
      return BytesValue.EMPTY;
    }

    MutableBytesValue output = MutableBytesValue.create(size);
    writeEncoded(output);
    return output;
  }
}
