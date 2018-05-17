package seed;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface TransactionDao {

  @UseClasspathSqlLocator
  @SqlScript
  void createTable();

  @SqlUpdate("INSERT INTO transaction (time, account_id, amount) VALUES (:timestampInMillis, :accountId, :amountInCents)")
  @GetGeneratedKeys
  long insert(@BindBean Transaction transaction);

  @SqlQuery("SELECT SUM(amount) FROM transaction WHERE account_id = ?")
  long balance(long accountId);
}
