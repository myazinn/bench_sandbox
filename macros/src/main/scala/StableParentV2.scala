import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object StableParentV2 {
  def getAllParents: String = macro getAllParentsImpl

  def getAllParentsImpl(c: Context): c.Expr[String] = {
    import c.universe._

    @tailrec
    def extractOwners(sym: Symbol, acc: List[Name]): List[Name] =
      if (sym == NoSymbol || sym.isPackage) acc
      else extractOwners(sym.owner, sym.name :: acc)

    val path = extractOwners(c.internal.enclosingOwner, Nil).mkString(".")

    c.Expr[String](Literal(Constant(path)))
  }
}
