/*
This is the defult vault database and you can create as many as you want for the
same tokenization instance.
 */
DROP TABLE IF EXISTS token_vault;

CREATE TABLE token_vault (
  id VARCHAR(1024) NOT NULL,
  value VARCHAR(1024) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX value_idx ON token_vault(value);

INSERT INTO token_vault(id, value) VALUES('4158281123', '4323927763');
INSERT INTO token_vault(id, value) VALUES('2123765411126543', '5645337854346543');
