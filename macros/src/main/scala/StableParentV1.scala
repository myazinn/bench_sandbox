import scala.annotation.tailrec
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros

object StableParentV1 {
  def getAllParents: String = macro getAllParentsImpl

  def getAllParentsImpl(c: Context): c.Expr[String] = {
    import c.universe._

    @tailrec
    def extractOwners(sym: Symbol, acc: List[Symbol]): List[Symbol] = {
      if (sym == NoSymbol || sym.isPackage || sym.isSynthetic || sym.name.toString.contains("$")) acc
      else extractOwners(sym.owner, sym :: acc)
    }

    val path = extractOwners(c.internal.enclosingOwner, Nil).map(_.name.toString.trim).mkString(".")
    c.Expr[String](Literal(Constant(path)))
  }
}
