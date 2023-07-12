import StableEmptyFieldsV2.emptyFieldNames

object Test extends App {

  final case class Foo(foo: Option[Bar])

  final case class Bar(bar: Option[Buzz])

  final case class Buzz(buzz: Option[Int])

  val foo1 = Foo(None)
  val foo2 = Foo(Some(Bar(None)))
  val foo3 = Foo(Some(Bar(Some(Buzz(None)))))
  val foo4 = Foo(Some(Bar(Some(Buzz(Some(1))))))

  val result1 = emptyFieldNames(foo1.foo.flatMap(_.bar).flatMap(_.buzz))
  println(result1) // Output: List(foo1.foo)
  assert(result1 == List("foo1.foo"))

  val result2 = emptyFieldNames(foo2.foo.flatMap(_.bar).flatMap(_.buzz))
  println(result2) // Output: List(foo2.foo.bar)
  assert(result2 == List("foo2.foo.bar"))

  val result3 = emptyFieldNames(foo3.foo.flatMap(_.bar).flatMap(_.buzz))
  println(result3) // Output: List(foo3.foo.bar.buzz)
  assert(result3 == List("foo3.foo.bar.buzz"))

  val result4 = emptyFieldNames(foo4.foo.flatMap(_.bar).flatMap(_.buzz))
  println(result4) // Output: List()
  assert(result4 == List())

  final case class Foo1(foo: Bar1)

  final case class Bar1(bar: Buzz1)

  final case class Buzz1(buzz: Option[Int])

  val foo11 = Foo1(Bar1(Buzz1(None)))
  val foo12 = Foo1(Bar1(Buzz1(Some(1))))

  val result11 = emptyFieldNames(foo11.foo.bar.buzz)
  println(result11) // Output: List(foo11.foo.bar.buzz)
  assert(result11 == List("foo11.foo.bar.buzz"))

  val result12 = emptyFieldNames(foo12.foo.bar.buzz)
  println(result12) // Output: List()
  assert(result12 == List())

}
