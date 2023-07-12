object MyEmptyFieldNamesUpdated {

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  def emptyFieldNames(fields: Option[Any]*): Seq[String] = macro emptyFieldNamesImpl

  def emptyFieldNamesImpl(c: Context)(fields: c.Expr[Option[Any]]*): c.Expr[Seq[String]] = {
    import c.universe._

    object UnwrapImplicit {
      def unapply(tree: c.Tree): Option[c.Tree] =
        tree match {
          case q"$wrapper($lhs)" if wrapper.symbol.isImplicit =>
            Some(lhs)
          case _ =>
            Some(tree)
        }
    }

    // Extract the names of the fields from the expressions
    def extractFieldNames(expr: Tree): Seq[String] = expr match {
      case Select(_, name) => Seq(name.decodedName.toString)
      case _ =>
        c.abort(c.enclosingPosition, s"Expected field selection, got $expr")
        Seq.empty
    }

    // Recursively flatten and extract the field names from nested options
    def extractNestedFieldNames(expr: Tree): Seq[String] = expr match {
      case q"$opt.flatMap[..$tpe](..$args)" =>
        Seq(s"$opt and $tpe and $args")
      case Function(left, right) =>
        Seq(s"$left and $right")
      case q"$opt.flatMap($f)" =>
        extractFieldNames(f) ++ extractNestedFieldNames(opt)
      case q"$opt.$name(..$f)" =>
        c.abort(c.enclosingPosition, s"wee hee $opt.$name($f)")
      case _ =>
        extractFieldNames(expr)
    }

    // Generate a sequence of field names that correspond to None values
    val result = fields.map { expr =>
      val nestedFieldNames = extractNestedFieldNames(expr.tree)

      if (nestedFieldNames.isEmpty) {
        val isNone = q"$expr.isEmpty"
        q"if ($isNone) Seq(${expr.tree.toString}) else Seq.empty"
      } else {
        val flatMapBindings = nestedFieldNames.dropRight(1).foldRight(expr.tree) { (fieldName, acc) =>
          if (fieldName == "_") q"$acc.map(_)"
          else q"$acc.flatMap(_.${TermName(fieldName)})"
        }

        val lastFieldName = nestedFieldNames.last
        val isNone = q"$flatMapBindings.flatMap(_.${TermName(lastFieldName)}).isEmpty"
        q"if ($isNone) Seq(${expr.tree.toString}) else Seq.empty"
      }
    }

    // Combine the sequences into a single result
    val combinedResult = result.foldLeft(q"Vector.empty[String]") { (acc, curr) =>
      q"$acc ++ $curr"
    }

    c.Expr[Seq[String]](combinedResult)
  }


}
