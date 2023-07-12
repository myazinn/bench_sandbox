object Test2 extends App {

  class Foo {
    def unused = ???
    def bar = {
      val x = 1
      val buzz = {
        val location: String = StableParentV2.getAllParents // location = "Test2.Foo.bar.buzz.location"
        println(StableParentV2.getAllParents)
        location
      }
      buzz
    }
  }

  println(new Foo().bar)
}
