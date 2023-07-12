import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object StableEmptyFieldsV2 {

  def emptyFieldNames(fields: Option[Any]*): Seq[String] = macro emptyFieldNamesImpl

  def emptyFieldNamesImpl(c: blackbox.Context)(fields: c.Expr[Option[Any]]*): c.Expr[Seq[String]] = {
    import c.universe._

    def pathToField(expr: Tree): List[Name] = {
      @tailrec
      def go(expr: Tree)(acc: List[Name]): List[Name] =
        expr match {
          case Select(s: Select, name) => go(s)(name.decodedName :: acc)
          case Select(_, name) => name.decodedName :: acc
          case _ =>
            val msg =
              s"""Expected field selection, got something else.
                 |Note: You can use `.flatMap` to extract nested optional fields as follows:
                 |variable.foo.flatMap(_.bar).flatMap(_.buzz)
                 |NOT
                 |variable.foo.flatMap(_.bar.flatMap(_.buzz))
                 |""".stripMargin
            c.abort(c.enclosingPosition, msg)
        }

      go(expr)(Nil)
    }

    def extractNestedFieldNames(expr: Tree): Tree = {

      def asText(path: List[Name]): Expr[String] = c.Expr(Literal(Constant(path.mkString("."))))

      def go(expr: Tree, pathPrefix: List[Name]): (List[Name], Tree) = {

        def fromSelect(selected: Tree, prefix: List[Name]): (List[Name], Tree) = {
          val fullPath = prefix ++ pathToField(selected)
          (fullPath, q"_root_.scala.Option.when($expr.isEmpty)(${asText(fullPath)})")
        }

        def fromFlatMap(qual: Tree, body: Tree): (List[Name], Tree) = {
          val (prefix1, left) = go(qual, pathPrefix)
          val (prefix2, right) = fromSelect(body, prefix1)
          (prefix2, q"$left.orElse($right)")
        }

        expr match {
          case q"$opt.flatMap[$_](${Function(_, body)})" => fromFlatMap(opt, body)
          case _ => fromSelect(expr, pathPrefix)
        }
      }

      go(expr, Nil)._2
    }

    val results = fields.map(expr => extractNestedFieldNames(expr.tree))

    val combinedResult =
      results.foldLeft(q"_root_.scala.Seq.empty[String]") { (acc, curr) =>
        q"$acc ++ $curr"
      }

    c.Expr[Seq[String]](combinedResult)
  }

}
