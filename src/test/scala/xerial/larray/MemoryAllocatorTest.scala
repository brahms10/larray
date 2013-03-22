//--------------------------------------
//
// MemoryAllocatorTest.scala
// Since: 2013/03/22 12:02
//
//--------------------------------------

package xerial.larray

/**
 * @author Taro L. Saito
 */
class MemoryAllocatorTest extends LArraySpec {
  "ConcurrentMemoryAllocator" should {
    "perform better than the default one in multi-threaded code" in {

      val N = 1000
      val B = 64 * 1024
      val m1 = new DefaultAllocator
      val m2 = new ConcurrentMemoryAllocator

      try {
        val t = time("alloc", repeat = 5) {
          block("default") {
            val l = for (i <- (0 until N).par) yield {
              new LIntArray(B)(m1)
            }
            l.foreach(_.free)
          }

          block("concurrent") {
            val l = for (i <- (0 until N).par) yield {
              new LIntArray(B)(m2)
            }
            l.foreach(_.free)
          }

          block("Array") {
            val l = for (i <- (0 until N).par) yield {
              new Array[Int](B)
            }
          }
        }
        t("concurrent") should be <= (t("default"))

      }
      finally {
        m1.releaseAll
        m2.releaseAll
      }
    }
  }
}