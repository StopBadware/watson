# abusive review requesters schema

# --- !Ups

CREATE TABLE abusive_requesters (
  email VARCHAR(256) PRIMARY KEY,
  flagged_by INTEGER NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  flagged_at TIMESTAMP NOT NULL DEFAULT NOW()
);

# --- !Downs

DROP TABLE IF EXISTS abusive_requesters CASCADE;