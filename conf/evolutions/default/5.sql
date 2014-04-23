# review request questions related schemas

# --- !Ups

CREATE TABLE request_questions (
  id SERIAL PRIMARY KEY,
  question VARCHAR(128) UNIQUE NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON request_questions (enabled);

CREATE TABLE request_answers (
  id SERIAL PRIMARY KEY,
  question_id INTEGER NOT NULL REFERENCES request_questions (id) ON DELETE RESTRICT,
  answer VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (question_id, answer)
);

CREATE INDEX ON request_answers (question_id, enabled);

CREATE TABLE request_responses (
  id SERIAL PRIMARY KEY,
  review_request_id INTEGER NOT NULL REFERENCES review_requests (id) ON DELETE RESTRICT,
  question_id INTEGER NOT NULL REFERENCES request_questions (id) ON DELETE RESTRICT,
  answer_id INTEGER DEFAULT NULL REFERENCES request_answers (id) ON DELETE RESTRICT,
  responded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON request_responses (question_id, answer_id);
CREATE INDEX ON request_responses (review_request_id);

# --- !Downs
DROP TABLE request_responses;
DROP TABLE request_answers;
DROP TABLE request_questions;