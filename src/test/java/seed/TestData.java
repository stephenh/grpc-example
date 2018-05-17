package seed;

import java.util.concurrent.atomic.AtomicLong;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class TestData {

  private static final AtomicLong nextDbId = new AtomicLong();

  /** Creates a new test database. */
  public static Jdbi newDb() {
    // Use an in-memory database, but DB_CLOSE_DELAY tells it to keep tables between connections
    // Making a brand new database per test method is probably excessive, but is cheap for now,
    // and means test methods could run in parallel, which sounds cool. If eventually too slow
    // would have to revisit.
    long dbId = nextDbId.incrementAndGet();
    Jdbi db = Jdbi.create("jdbc:h2:mem:test" + dbId + ";DB_CLOSE_DELAY=-1");
    db.registerRowMapper(ProtoBeanMapper.factory(Account.class, Account.Builder.class));
    db.installPlugin(new SqlObjectPlugin());
    // Currently just re-issue all of the DML each time. In production would use migrations, e.g.
    // https://github.com/stephenh/joist/blob/master/features/src/migrations/java/features/migrations/m0001.java
    db.useExtension(AccountDao.class, dao -> dao.createTable());
    db.useExtension(TransactionDao.class, dao -> dao.createTable());
    return db;
  }

  // Kind of ugly, but add some helpers to make our tests prettier to read
  public static Account save(Jdbi db, Account.Builder account) {
    if (account.getId() == 0) {
      long id = db.withExtension(AccountDao.class, dao -> dao.insert(account.build()));
      return account.setId(id).build();
    } else {
      db.useExtension(AccountDao.class, dao -> dao.update(account.build()));
      return account.build();
    }
  }

  public static Account read(Jdbi db, Account account) {
    return db.withExtension(AccountDao.class, dao -> dao.read(account.getId()));
  }

  public static double balance(Jdbi db, Account account) {
    return ((Long) db.withExtension(TransactionDao.class, dao -> dao.balance(account.getId()))).longValue() / 100.00;
  }

  public static void deposit(Jdbi db, Account account, double dollars) {
    db.useExtension(TransactionDao.class, dao -> {
      dao.insert(
        Transaction
          .newBuilder()
          .setAccountId(account.getId())
          .setTimestampInMillis(System.currentTimeMillis()) // should use a clock
          .setAmountInCents((long) (dollars * 100.00))
          .build());
    });
  }

  public static Account.Builder newAccount() {
    return Account.newBuilder().setName("bob").setAddress("123 Road").setSsn("111-111-1111");
  }

}
