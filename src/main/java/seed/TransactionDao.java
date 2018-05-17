package seed;

import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlScript;

public interface TransactionDao {

  @UseClasspathSqlLocator
  @SqlScript
  void createTable();

}
