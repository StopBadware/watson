# community_reports related schemas

# --- !Ups

CREATE TABLE community_report_types (
  id SERIAL PRIMARY KEY,
  cr_type VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON community_report_types (cr_type);

CREATE TABLE community_reports (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  ip BIGINT DEFAULT NULL,
  description VARCHAR(2048) DEFAULT NULL,
  bad_code VARCHAR(2048) DEFAULT NULL,
  cr_type_id INTEGER DEFAULT NULL REFERENCES community_report_types (id) ON DELETE SET NULL,
  sbw_note VARCHAR(512) DEFAULT NULL,
  reported_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON community_reports (uri_id);
CREATE INDEX ON community_reports (cr_type_id, reported_at);

# --- !Downs

DROP TABLE IF EXISTS community_reports CASCADE;
DROP TABLE IF EXISTS community_report_types CASCADE;