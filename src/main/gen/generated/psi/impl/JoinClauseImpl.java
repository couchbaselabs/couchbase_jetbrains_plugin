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

public class JoinClauseImpl extends SqlppPSIWrapper implements JoinClause {

  public JoinClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull Visitor visitor) {
    visitor.visitJoinClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof Visitor) accept((Visitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public AnsiJoinClause getAnsiJoinClause() {
    return findChildByClass(AnsiJoinClause.class);
  }

  @Override
  @Nullable
  public IndexJoinClause getIndexJoinClause() {
    return findChildByClass(IndexJoinClause.class);
  }

  @Override
  @Nullable
  public LookupJoinClause getLookupJoinClause() {
    return findChildByClass(LookupJoinClause.class);
  }

}
