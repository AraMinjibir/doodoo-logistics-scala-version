-- !Ups

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
  weight NUMERIC  NOT NULL,
  length NUMERIC  NOT NULL,
  width NUMERIC  NOT NULL,
  height NUMERIC  NOT NULL,
  contents TEXT,
  status TEXT NOT NULL,
  estimated_delivery_date TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  cost NUMERIC(12,2) NOT NULL,
  history TEXT
);

CREATE INDEX idx_shipments_status ON shipments(status);

-- !Downs

DROP INDEX IF EXISTS idx_shipments_status;
DROP TABLE IF EXISTS shipments;
