object StableEmptyFieldsV1 {

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  def emptyFieldNames(fields: Option[Any]*): Seq[String] = macro emptyFieldNamesImpl

  def emptyFieldNamesImpl(c: Context)(fields: c.Expr[Option[Any]]*): c.Expr[Seq[String]] = {
    import c.universe._


    // Extract the names of the fields from the expressions
    def extractFieldNameStr(expr: Tree): String =
      expr match {
        case Select(s: Select, right) => extractFieldNameStr(s) + "." + right.decodedName
        case Select(_, name) => name.decodedName.toString
        case _ => expr.toString
      }

    // Extract the names of the fields from the expressions
    def extractFieldName(expr: Tree): Expr[String] =
      c.Expr(Literal(Constant(extractFieldNameStr(expr))))

    // Recursively flatten and extract the field names from nested options
    def extractNestedFieldNames(expr: Tree, prefix: Option[String]): (String, Tree) = {

      def handleSimple(prefix: Option[String]): (String, Tree) = {
        val fullPath = List(prefix, Some(extractFieldNameStr(expr))).flatten.mkString(".")
        (fullPath, q"Option.when($expr.isEmpty)(${c.Expr(Literal(Constant(fullPath)))})")
      }

      def handleFlatMap(prefix: Option[String], opt: Tree, body: Tree): (String, Tree) = {
        val (prefix1, left) = extractNestedFieldNames(opt, prefix)
        val (prefix2, right) = {
          val fullPath = List(Some(prefix1), Some(extractFieldNameStr(body))).flatten.mkString(".")
          (fullPath, q"Option.when($expr.isEmpty)(${c.Expr(Literal(Constant(fullPath)))})")
        }
        (prefix2, q"$left.orElse($right)")
      }

      expr match {
        case q"$opt.flatMap[$_](${Function(_, body)})" => handleFlatMap(prefix, opt, body)
        case _ => handleSimple(prefix)
      }

    }

    // Generate a sequence of field names that correspond to None values
    val result = fields.map { expr =>
      extractNestedFieldNames(expr.tree, None)._2
    }

    // Combine the sequences into a single result
    val combinedResult = result.foldLeft(q"Seq.empty[String]") { (acc, curr) =>
      q"$acc ++ $curr"
    }

    c.Expr[Seq[String]](combinedResult)
  }

}
