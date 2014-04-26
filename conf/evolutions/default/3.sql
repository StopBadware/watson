# email_templates schema

# --- !Ups

CREATE TABLE email_templates (
  name VARCHAR(24) PRIMARY KEY,
  subject VARCHAR(128) NOT NULL,
  body VARCHAR(4096) NOT NULL,
  modified_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE SET NULL,
  modified_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO email_templates (name, subject, body) VALUES  
  ('NoLongerBlacklisted', '[StopBadware] replace me', 'Use [URI] and/or [SAFE_URI] to insert links to URI or Clearinghouse entry'), 
  ('ReviewClosedBad', '[StopBadware] replace me', 'Use [URI] and/or [SAFE_URI] to insert links to URI or Clearinghouse entry and [BAD_CODE] for comments from reviewer'),
  ('ReviewClosedCleanTts', '[StopBadware] replace me', 'Use [URI] and/or [SAFE_URI] to insert links to URI or Clearinghouse entry'), 
  ('ReviewRequestReceived', '[StopBadware] replace me', 'Use [URI] and/or [SAFE_URI] to insert links to URI or Clearinghouse entry');

# --- !Downs

DROP TABLE IF EXISTS email_templates CASCADE;