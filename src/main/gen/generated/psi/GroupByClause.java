// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface GroupByClause extends PsiElement {

  @NotNull
  List<GroupTerm> getGroupTermList();

  @Nullable
  HavingClause getHavingClause();

  @Nullable
  LettingClause getLettingClause();

}
