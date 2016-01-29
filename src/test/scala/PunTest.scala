import org.scalatest._

class PunTest extends FunSuite {
  test("punScore") {
    assert(Pun.punScore("youness", "nessie") == 4)
  }
  test("overlapSize same") {
    assert(Pun.punScore("asdf", "asdf") == 4)
  }
  test("overlapSize missing first") {
    assert(Pun.punScore("", "asdf") == 0)
  }
  test("overlapSize missing second") {
    assert(Pun.punScore("asdf", "") == 0)
  }
}