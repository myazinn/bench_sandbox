object EmptyFieldNames {

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  def emptyFieldNames(fields: Option[Any]*): Seq[String] = macro emptyFieldNamesImpl

  def emptyFieldNamesImpl(c: Context)(fields: c.Expr[Option[Any]]*): c.Expr[Seq[String]] = {
    import c.universe._

    // Extract the names of the fields from the expressions
    val fieldNames = fields.map { expr =>
      expr.tree match {
        case Select(_, name) => expr -> name.decodedName.toString
        case _ => c.abort(c.enclosingPosition, "Expected field selection")
      }
    }

    // Generate a sequence of field names that correspond to None values
    val result = fieldNames.map {
      case (name, expr) =>
        val isNone = q"$expr.isEmpty"
        val fieldName = Literal(Constant(name))
        q"if ($isNone) Seq($fieldName) else Seq.empty"
    }

    // Combine the sequences into a single result
    val combinedResult = result.foldLeft(q"Seq.empty[String]") { (acc, curr) =>
      q"$acc ++ $curr"
    }

    c.Expr[Seq[String]](combinedResult)
  }


}
