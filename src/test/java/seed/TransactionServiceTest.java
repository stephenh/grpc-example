package seed;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static seed.TestData.balance;
import static seed.TestData.deposit;
import static seed.TestData.save;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TransactionServiceTest {

  private static final ZonedDateTime dec31 = ZonedDateTime.of(1999, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
  private static final ZonedDateTime jan1 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  private static final ZonedDateTime jan5 = ZonedDateTime.of(2000, 1, 5, 0, 0, 0, 0, ZoneOffset.UTC);
  private static final ZonedDateTime jan10 = ZonedDateTime.of(2000, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC);
  private static final ZonedDateTime jan20 = ZonedDateTime.of(2000, 1, 20, 0, 0, 0, 0, ZoneOffset.UTC);

  private Jdbi db;
  private TransactionService service;

  @Before
  public void setup() {
    db = Database.newDb();
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

  @Test
  public void shouldSearchTransactionsByAmount() {
    // given an account with a $5, $10, and $15 transaction
    Account a = save(db, TestData.newAccount());
    Transaction t5 = deposit(db, a, 5.00);
    Transaction t10 = deposit(db, a, 10.00);
    Transaction t15 = deposit(db, a, 15.00);
    // when we search for >=$10, we get two txns
    assertThat(search(a, req -> req.setMinimumAmountInCents(cents(10.00))), contains(t10, t15));
    // and when we search for <=$10, we get two txns
    assertThat(search(a, req -> req.setMaximumAmountIncents(cents(10.00))), contains(t5, t10));
    // and when we search >=$20, we get zero transactions
    assertThat(search(a, req -> req.setMinimumAmountInCents(cents(20.00))), empty());
    // and when we search <=$4, we get zero transactions
    assertThat(search(a, req -> req.setMaximumAmountIncents(cents(4.00))), empty());
    // TODO should test negative transactions
  }

  @Test
  public void shouldSearchTransactionsByDate() {
    // given an account with transactions on jan1, jan5, and jan10
    Account a = save(db, TestData.newAccount());
    Transaction t1 = deposit(db, a, 1.00, t -> t.setTimestampInMillis(millis(jan1)));
    Transaction t5 = deposit(db, a, 1.00, t -> t.setTimestampInMillis(millis(jan5)));
    Transaction t10 = deposit(db, a, 1.00, t -> t.setTimestampInMillis(millis(jan10)));
    // when we search for >= jan5, we get two txns
    assertThat(search(a, req -> req.setMinimumTimestamp(millis(jan5))), contains(t5, t10));
    // and when we search for <= jan5, we get two txns
    assertThat(search(a, req -> req.setMaximumTimestamp(millis(jan5))), contains(t1, t5));
    // and when we search >= jan20, we get zero transactions
    assertThat(search(a, req -> req.setMinimumTimestamp(millis(jan20))), empty());
    // and when we search <= dec31, we get zero transactions
    assertThat(search(a, req -> req.setMaximumTimestamp(millis(dec31))), empty());
  }

  @Test
  public void shouldSearchTransactionsAmountAndByDate() {
    // given an account with transactions on jan5 of $5 and $10
    Account a = save(db, TestData.newAccount());
    deposit(db, a, 5.00, t -> t.setTimestampInMillis(millis(jan5)));
    Transaction t2 = deposit(db, a, 10.00, t -> t.setTimestampInMillis(millis(jan5)));
    // when we search for >= jan5 and >= $10, we get back only the tax
    assertThat(search(a, req -> {
      req.setMinimumTimestamp(millis(jan5));
      req.setMinimumAmountInCents(cents(10.00));
    }), contains(t2));
  }

  @Test
  @Ignore
  public void shouldFailGetTransactionForAnInvalidId() {
    // Skipping for now
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

  private List<Transaction> search(Account account, Consumer<SearchTransactionsRequest.Builder> f) {
    SearchTransactionsRequest.Builder req = SearchTransactionsRequest.newBuilder().setAccountId(account.getId());
    f.accept(req);
    SearchTransactionsResponse res = StubObserver.<SearchTransactionsResponse> getSync(o -> service.searchInAccount(req.build(), o));
    return res.getTransactionsList();
  }

  private static long cents(double dollars) {
    return (long) (dollars * 100);
  }

  private static long millis(ZonedDateTime d) {
    return d.toInstant().toEpochMilli();
  }

}
