package seed;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static seed.TestData.balance;
import static seed.TestData.deposit;
import static seed.TestData.save;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TransactionServiceTest {

  private Jdbi db;
  private TransactionService service;

  @Before
  public void setup() {
    db = TestData.newDb();
    service = new TransactionService(db);
  }

  // I almost went down the rabbit hole of previewing JDK10 which finally finally
  // (finally) brings `var` to Java. I have a mostly-love/slightly-hate relationship
  // with Scala. Could have tried Kotlin, what's another experimental technology in
  // the mix for a graded assignment?

  @Test
  public void shouldCreateATransfer() {
    // given two existing accounts
    Account a1 = save(db, TestData.newAccount());
    Account a2 = save(db, TestData.newAccount());
    // and a1 has $100 in it
    deposit(db, a1, 100.00);
    // when we transfer $100 from a1 to a2
    TransferResponse rep = transfer(a1, a2, 100.00, "transfer 1");
    // then it's a success
    assertThat(rep.getSuccess(), is(true));
    // and the balances have changed
    assertThat(balance(db, a1), is(0.00));
    assertThat(balance(db, a2), is(100.00));
    // and the source transaction looks good
    Transaction t1 = db.withExtension(TransactionDao.class, dao -> dao.read(2L));
    assertThat(t1.getAccountId(), is(a1.getId()));
    assertThat(t1.getAmountInCents(), is(-10000L));
    assertThat(t1.getDescription(), is("transfer 1"));
    // and the destination transaction looks good
    Transaction t2 = db.withExtension(TransactionDao.class, dao -> dao.read(3L));
    assertThat(t2.getAccountId(), is(a2.getId()));
    assertThat(t2.getAmountInCents(), is(10000L));
    assertThat(t2.getDescription(), is("transfer 1"));
  }

  @Test
  public void shouldNotAllowAnOverride() {
    // given two existing accounts
    Account a1 = save(db, TestData.newAccount());
    Account a2 = save(db, TestData.newAccount());
    // and a1 has $10 in it
    deposit(db, a1, 10.00);
    // when we transfer $100 from a1 to a2
    TransferResponse rep = transfer(a1, a2, 100.00, "transfer 1");
    // then it fails
    assertThat(rep.getSuccess(), is(false));
    assertThat(rep.getErrorsList(), hasItems("Insufficient funds"));
    // and the balances have not changed
    assertThat(balance(db, a1), is(10.00));
    assertThat(balance(db, a2), is(0.00));
  }

  @Test
  public void shouldGetTransactionsForAnAccount() {
    // given an account with several transactions
    Account a = save(db, TestData.newAccount());
    deposit(db, a, 1.00);
    deposit(db, a, 2.00);
    // when we get the transactions
    GetByAccountResponse rep = getByAccount(a);
    // then we see both transactions
    assertThat(rep.getTransactionsCount(), is(2));
    Transaction t1 = rep.getTransactions(0);
    assertThat(t1.getDescription(), is("deposit"));
    assertThat(t1.getAmountInCents(), is(100L));
    // and the second transaction as well
    Transaction t2 = rep.getTransactions(1);
    assertThat(t2.getDescription(), is("deposit"));
    assertThat(t2.getAmountInCents(), is(200L));
  }

  @Test
  @Ignore
  public void shouldFailGetTransactionsForAnInvalidAccount() {
    // Skipping for now
  }

  @Test
  public void shouldGetTransaction() {
    // given an account with one transaction
    Account a = save(db, TestData.newAccount());
    Transaction t = deposit(db, a, 1.00);
    // when we get that transaction
    GetTransactionResponse rep = get(t.getId());
    // then it matches
    assertThat(rep.getTransaction(), is(t));
  }

  private TransferResponse transfer(Account from, Account dest, double dollars, String description) {
    TransferRequest req = TransferRequest
      .newBuilder()
      .setSourceAccountId(from.getId())
      .setDestinationAccountId(dest.getId())
      .setAmountInCents((int) (dollars * 100))
      .setDescription(description)
      .build();
    return StubObserver.<TransferResponse> getSync(o -> service.transfer(req, o));
  }

  private GetByAccountResponse getByAccount(Account from) {
    GetByAccountRequest req = GetByAccountRequest.newBuilder().setAccountId(from.getId()).build();
    return StubObserver.<GetByAccountResponse> getSync(o -> service.getByAccount(req, o));
  }

  private GetTransactionResponse get(long id) {
    GetTransactionRequest req = GetTransactionRequest.newBuilder().setTransactionId(id).build();
    return StubObserver.<GetTransactionResponse> getSync(o -> service.get(req, o));
  }

}
