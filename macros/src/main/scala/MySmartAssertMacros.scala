////import scala.reflect.macros.blackbox
//
//class MySmartAssertMacros(val c: blackbox.Context) {
//
//  import c.universe._
//
//  def missingFields_impl(exprs: c.Expr[Option[Any]]*): c.Expr[Seq[String]] = {
//    exprs.map { expr =>
//      val tree = expr.tree
//
//      val parsed = parseExpr(tree)
//      val ast = astToAssertion(parsed)
//
//      val block =
//        q"""
//  $TestResult($ast.withCode($codeString).withLocation)
//          """
//
//      block
//    }
//
//  }
//
//  sealed trait AST
//
//  object AST {
//    case class Method(
//                       lhs: AST,
//                       lhsTpe: Type,
//                       rhsTpe: Type,
//                       name: String,
//                       tpes: List[Type],
//                       args: Option[List[c.Tree]]
//                     ) extends AST
//
//    case class Function(lhs: c.Tree, rhs: AST, lhsTpe: Type) extends AST
//
//    case class Raw(ast: c.Tree) extends AST
//  }
//
//  def astToAssertion(ast: AST): c.Tree =
//    ast match {
//      case AST.Method(lhs, lhsTpe, _, "flatMap", _, Some(args)) if lhsTpe <:< weakTypeOf[Option[_]] =>
//        val assertion = astToAssertion(parseExpr(args.head))
//        q"if ($lhs.isEmpty) Option.empty[String] else $assertion"
//
//      case AST.Function(_, _, _) =>
//        q"Option.empty[String]"
//
//      case AST.Raw(ast) =>
//        q"Option.empty[String]"
//    }
//
//  def parseExpr(tree: c.Tree): AST =
//    tree match {
//      case MethodCall(lhs, name, tpes, args) =>
//        AST.Method(
//          parseExpr(lhs),
//          lhs.tpe.widen,
//          tree.tpe.widen,
//          name.toString,
//          tpes,
//          args
//        )
//
//      case fn@q"($a) => $b" =>
//        val inType = fn.tpe.widen.typeArgs.head
//        AST.Function(a, parseExpr(b), inType)
//
//      case _ => AST.Raw(tree)
//    }
//
//  object UnwrapImplicit {
//    def unapply(tree: c.Tree): Option[c.Tree] =
//      tree match {
//        case q"$wrapper($lhs)" if wrapper.symbol.isImplicit =>
//          Some(lhs)
//        case _ => Some(tree)
//      }
//  }
//
//  object MethodCall {
//    def unapply(tree: c.Tree): Option[(c.Tree, TermName, List[Type], Option[List[c.Tree]])] =
//      tree match {
//        case q"${UnwrapImplicit(lhs)}.$name[..$tpes]"
//          if !(tree.symbol.isModule || tree.symbol.isStatic || tree.symbol.isClass) =>
//          Some((lhs, name, tpes.map(_.tpe), None))
//        case q"${UnwrapImplicit(lhs)}.$name"
//          if !(tree.symbol.isModule || tree.symbol.isStatic || tree.symbol.isClass) =>
//          Some((lhs, name, List.empty, None))
//        case q"${UnwrapImplicit(lhs)}.$name(..$args)" =>
//          Some((lhs, name, List.empty, Some(args)))
//        case q"${UnwrapImplicit(lhs)}.$name[..$tpes](..$args)" =>
//          Some((lhs, name, tpes.map(_.tpe), Some(args)))
//        case _ => None
//      }
//  }
//
//}
