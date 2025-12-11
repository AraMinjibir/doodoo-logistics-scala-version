# --- !Ups

CREATE TABLE shipments (
  id uuid PRIMARY KEY,
  tracking_number TEXT,
  sender_name TEXT NOT NULL,
  recipient_name TEXT NOT NULL,
  recipient_street TEXT NOT NULL,
  recipient_city TEXT NOT NULL,
  recipient_state TEXT NOT NULL,
  recipient_country TEXT NOT NULL,
  recipient_postal_code TEXT NOT NULL,
  recipient_contact TEXT NOT NULL,
  weight DOUBLE PRECISION NOT NULL,
  length DOUBLE PRECISION NOT NULL,
  width DOUBLE PRECISION NOT NULL,
  height DOUBLE PRECISION NOT NULL,
  contents TEXT,
  status TEXT NOT NULL,
  estimated_delivery_date TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  cost NUMERIC(12,2) NOT NULL,
  history TEXT
);

CREATE INDEX idx_shipments_status ON shipments(status);

# --- !Downs

DROP INDEX IF EXISTS idx_shipments_status;
DROP TABLE IF EXISTS shipments;
