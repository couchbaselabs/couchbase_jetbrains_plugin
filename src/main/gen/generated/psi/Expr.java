// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface Expr extends PsiElement {

  @NotNull
  List<ArithmeticTerm> getArithmeticTermList();

  @Nullable
  CaseExpr getCaseExpr();

  @NotNull
  List<CollectionExpr> getCollectionExprList();

  @NotNull
  List<ComparisonTerm> getComparisonTermList();

  @NotNull
  List<ConcatenationTerm> getConcatenationTermList();

  @Nullable
  FunctionCall getFunctionCall();

  @Nullable
  IdentifierRef getIdentifierRef();

  @Nullable
  JsonValue getJsonValue();

  @Nullable
  Literal getLiteral();

  @NotNull
  List<LogicalTerm> getLogicalTermList();

  @Nullable
  NamedArg getNamedArg();

  @Nullable
  NestedExpr getNestedExpr();

  @Nullable
  Path getPath();

  @Nullable
  SubqueryExpr getSubqueryExpr();

}
