import org.scalatest._
import scala.collection.immutable.TreeSet

class PunTest extends FunSuite {
  test("PunTest youness-nessie") {
    assert(Pun.punScore("youness", "nessie", "youness", "nessie") == 4)
  }
  test("PunTest same") {
    assert(Pun.punScore("asdf", "asdf", "asdf", "asdf") == 0)
  }
  test("PunTest same word with s") {
    assert(Pun.punScore("asdfs", "asdf", "aesdfs", "aesdf") == 0)
  }
  test("PunTest missing word") {
    assert(Pun.punScore("asdf", "", "asdf", "") == 0)
  }
  test("PunTest caterpillar-larceny") {
    assert(Pun.punScore("caterpillar", "larceny", "caterpillar", "larceny") == 3)
  }
}