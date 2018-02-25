package net.develgao.beaconchain.ethereum.rlp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.develgao.beaconchain.util.bytes.Bytes32;
import net.develgao.beaconchain.util.bytes.BytesValue;
import net.develgao.beaconchain.util.bytes.BytesValues;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * An {@link RLPInput} that reads RLP encoded data from a {@link File}.
 */
public class FileRLPInput extends AbstractRLPInput {

  // The RLP encoded data.
  private final FileChannel file;

  public FileRLPInput(FileChannel file, boolean lenient) throws IOException {
    super(lenient);
    checkNotNull(file);
    checkArgument(file.isOpen());
    this.file = file;

    init(file.size(), false);
  }

  @Override
  protected byte inputByte(long offset) {
    try {
      ByteBuffer buf = ByteBuffer.wrap(new byte[1]);

      file.read(buf, offset);
      byte b = buf.get(0);
      return b;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected BytesValue inputSlice(long offset, int length) {
    try {
      byte[] bytes = new byte[length];
      ByteBuffer buf = ByteBuffer.wrap(bytes);
      file.read(buf, offset);
      return BytesValue.of(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Bytes32 inputSlice32(long offset) {
    return Bytes32.wrap(inputSlice(offset, 32), 0);
  }

  @Override
  protected String inputHex(long offset, int length) {
    return inputSlice(offset, length).toString().substring(2);
  }

  @Override
  protected BigInteger getUnsignedBigInteger(long offset, int length) {
    return BytesValues.asUnsignedBigInteger(inputSlice(offset, length));
  }

  @Override
  protected int getInt(long offset) {
    return inputSlice(offset, Integer.BYTES).getInt(0);
  }

  @Override
  protected long getLong(long offset) {
    return inputSlice(offset, Long.BYTES).getLong(0);

  }

  @Override
  public BytesValue raw() {
    throw new UnsupportedOperationException("raw() not supported on a Channel");
  }

  /**
   * @return Offset of the current item
   */
  public long currentOffset() {
    return currentItem;
  }

  @Override
  public void setTo(long item) {
    super.setTo(item);
  }
}
