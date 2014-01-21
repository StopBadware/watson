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
  ('NoLongerBlacklisted', '[StopBadware] subject', 'Use [URI] or [SAFE_URI] to insert links to URI or Clearinghouse entry'), 
  ('ReviewClosedBad', '[StopBadware] subject', 'Use [URI] or [SAFE_URI] to insert links to URI or Clearinghouse entry and [TESTER_NOTES] for comments from reviewer'),
  ('ReviewClosedCleanTts', '[StopBadware] subject', 'Use [URI] or [SAFE_URI] to insert links to URI or Clearinghouse entry'), 
  ('ReviewRequestReceived', '[StopBadware] subject', 'Use [URI] or [SAFE_URI] to insert links to URI or Clearinghouse entry');

# --- !Downs

DROP TABLE email_templates CASCADE;