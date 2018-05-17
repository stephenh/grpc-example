
-- Don't really like the name "transaction", as to me that implies multiple
-- entries; I considered calling this "account_entry" and then making "transaction"
-- an entity that ties multiple account entries together (e.g. both sides of a
-- transfer), but then the temptation is to make transactins double-entry, which
-- is not a good idea as this isn't an accounting system. So avoiding all of that.
CREATE TABLE transaction (
  id INT AUTO_INCREMENT,
  -- Would prefer using a timestamp/something but h2 doesn't map millis directly, so punting
  time BIGINT NOT NULL,
  account_id INT NOT NULL,
  amount BIGINT NOT NULL,
  FOREIGN KEY (account_id) REFERENCES account (id));