# email_templates schema

# --- !Ups

CREATE TABLE email_templates (
  name VARCHAR(20) PRIMARY KEY,
  subject VARCHAR(128) NOT NULL,
  body VARCHAR(4096) NOT NULL,
  modified_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE SET NULL,
  modified_at TIMESTAMP NOT NULL DEFAULT NOW()
);

# --- !Downs

DROP TABLE email_templates CASCADE;