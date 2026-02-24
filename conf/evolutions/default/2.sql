-- !Ups

CREATE TABLE support_center(
    id UUID PRIMARY KEY,
    user_id UUID,
    shipment_id UUID,

    subject TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    resolved_by UUID NULL,

    comment TEXT,

    CONSTRAINT support_center_shipment_fk
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_support_center_status
    ON support_center(status);

-- !Downs

DROP INDEX IF EXISTS idx_support_center_status;
DROP TABLE IF EXISTS support_center;