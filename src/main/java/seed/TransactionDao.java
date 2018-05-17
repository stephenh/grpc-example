package seed;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
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

  @SqlUpdate("INSERT INTO transaction (time, account_id, amount, description) VALUES (:timestampInMillis, :accountId, :amountInCents, :description)")
  @GetGeneratedKeys
  long insert(@BindBean Transaction transaction);

  @SqlQuery("SELECT id, account_id AS accountId, amount AS amountInCents, description FROM transaction WHERE id = ?")
  Transaction read(long id);

  @SqlQuery("SELECT id, account_id AS accountId, amount AS amountInCents, description FROM transaction WHERE account_id = ? ORDER BY id")
  List<Transaction> readForAccount(long accountId);

  @SqlQuery("SELECT SUM(amount) FROM transaction WHERE account_id = ?")
  long balance(long accountId);

  @SqlQuery("SELECT id, account_id AS accountId, amount AS amountInCents, description" //
    + " FROM transaction"
    + " WHERE account_id = :accountId"
    + " AND (:minTimeInMillis IS NULL OR time >= :minTimeInMillis)"
    + " AND (:maxTimeInMillis IS NULL OR time <= :maxTimeInMillis)"
    + " AND (:minAmountInCents IS NULL OR amount >= :minAmountInCents)"
    + " AND (:maxAmountInCents IS NULL OR amount <= :maxAmountInCents)"
    + " ORDER BY id")
  List<Transaction> search(
    @Bind("accountId") long accountId,
    @Bind("minTimeInMillis") Optional<Long> minTimeInMillis,
    @Bind("maxTimeInMillis") Optional<Long> maxTimeInMillis,
    @Bind("minAmountInCents") Optional<Long> minAmountInCents,
    @Bind("maxAmountInCents") Optional<Long> maxAmountInCents);
}
