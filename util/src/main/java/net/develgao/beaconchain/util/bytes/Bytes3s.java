package net.develgao.beaconchain.util.bytes;

/** Static utility methods to work with {@link Bytes3}. */
public class Bytes3s {
  private Bytes3s() {}

  public static void and(Bytes3 v1, Bytes3 v2, MutableBytes3 result) {
    for (int i = 0; i < Bytes3.SIZE; i++) {
      result.set(i, (byte) (v1.get(i) & v2.get(i)));
    }
  }

  public static Bytes3 and(Bytes3 v1, Bytes3 v2) {
    MutableBytes3 mb3 = MutableBytes3.create();
    and(v1, v2, mb3);
    return mb3;
  }

  public static void or(Bytes3 v1, Bytes3 v2, MutableBytes3 result) {
    for (int i = 0; i < Bytes3.SIZE; i++) {
      result.set(i, (byte) (v1.get(i) | v2.get(i)));
    }
  }

  public static Bytes3 or(Bytes3 v1, Bytes3 v2) {
    MutableBytes3 mb3 = MutableBytes3.create();
    or(v1, v2, mb3);
    return mb3;
  }

  public static void xor(Bytes3 v1, Bytes3 v2, MutableBytes3 result) {
    for (int i = 0; i < Bytes3.SIZE; i++) {
      result.set(i, (byte) (v1.get(i) ^ v2.get(i)));
    }
  }

  public static Bytes3 xor(Bytes3 v1, Bytes3 v2) {
    MutableBytes3 mb3 = MutableBytes3.create();
    xor(v1, v2, mb3);
    return mb3;
  }

  public static void not(Bytes3 v, MutableBytes3 result) {
    for (int i = 0; i < Bytes3.SIZE; i++) {
      result.set(i, (byte) (~v.get(i)));
    }
  }

  public static Bytes3 not(Bytes3 v) {
    MutableBytes3 mb3 = MutableBytes3.create();
    not(v, mb3);
    return mb3;
  }

  public static String unprefixedHexString(Bytes3 v) {
    return v.toString().substring(2);
  }
}
