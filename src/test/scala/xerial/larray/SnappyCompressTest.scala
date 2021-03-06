//--------------------------------------
//
// SnappyCompressTest.scala
// Since: 2013/03/27 16:17
//
//--------------------------------------

package xerial.larray

import org.xerial.snappy.Snappy
import scala.util.Random
import java.io.File

/**
 * @author Taro L. Saito
 */
class SnappyCompressTest extends LArraySpec {

  implicit class AsRaw[A](l:LArray[A]) {
    def address = l.asInstanceOf[RawByteArray[Int]].address
  }

  "Snappy" should {

    "compress LArray" in {
      val l = (for (i <- 0 until 3000) yield math.toDegrees(math.sin(i/360)).toInt).toLArray
      val maxLen = Snappy.maxCompressedLength(l.byteLength.toInt)
      val compressedBuf = LArray.of[Byte](maxLen)
      val compressedLen = Snappy.rawCompress(l.address, l.byteLength, compressedBuf.address)

      val compressed = compressedBuf.slice(0, compressedLen)
      val uncompressedLength = Snappy.uncompressedLength(compressed.address, compressed.byteLength)
      val uncompressed = LArray.of[Int](uncompressedLength / 4)
      Snappy.rawUncompress(compressed.address, compressed.byteLength, uncompressed.address)

      debug(s"byteLength:${l.byteLength}, max compressed length:$maxLen ,compressed length:$compressedLen")
      l.sameElements(uncompressed) should be (true)
    }

    "compress LIntArray" taggedAs("it") in {
      val N = 100000000L
      val l = new LIntArray(N)
      debug(f"preparing data set. N=$N%,d")
      for(i <- 0 Until N) l(i) =  math.toDegrees(math.sin(i/360)).toInt

      debug("compressing the data")
      val maxLen = Snappy.maxCompressedLength(l.byteLength.toInt)
      val compressedBuf = LArray.of[Byte](maxLen)
      val compressedLen = Snappy.rawCompress(l.address, l.byteLength, compressedBuf.address)
      val compressed = compressedBuf.slice(0, compressedLen)
      val f = File.createTempFile("snappy", ".dat", new File("target"))
      f.deleteOnExit()
      compressed.saveTo(f)

      debug("decompressing the data")
      val b = LArray.mmap(f, 0, f.length, MMapMode.READ_ONLY)
      val len = Snappy.uncompressedLength(b.address, b.length)
      val decompressed = new LIntArray(len / 4)
      Snappy.rawUncompress(b.address, b.length, decompressed.address)

      debug(f"l.length:${l.length}%,d, decompressed.length:${decompressed.length}%,d")

      l.sameElements(decompressed) should be (true)
      info("start bench")
      time("iterate", repeat=10) {
        block("new array") {
          var i = 0
          while(i < l.length) {
            l(i)
            i += 1
          }
        }

        block("decompressed") {
          var i = 0
          while(i < decompressed.length) {
            decompressed(i)
            i += 1
          }
        }
      }
    }
  }
}