// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static generated.GeneratedTypes.*;
import org.intellij.sdk.language.psi.SqlppPSIWrapper;
import generated.psi.*;

public class NestClauseImpl extends SqlppPSIWrapper implements NestClause {

  public NestClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull Visitor visitor) {
    visitor.visitNestClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof Visitor) accept((Visitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public AnsiNestClause getAnsiNestClause() {
    return findChildByClass(AnsiNestClause.class);
  }

  @Override
  @Nullable
  public IndexNestClause getIndexNestClause() {
    return findChildByClass(IndexNestClause.class);
  }

  @Override
  @Nullable
  public LookupNestClause getLookupNestClause() {
    return findChildByClass(LookupNestClause.class);
  }

}
