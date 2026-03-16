        -- !Ups

        CREATE TABLE payments(
        customer_id UUID NOT NULL,
        shipment_id UUID NOT NULL,
        amount NUMERIC(12,2) NOT NULL,

        status TEXT NOT NULL,
        paid_at TIMESTAMP WITH TIME ZONE NOT NULL,
        payment_method  TEXT NOT NULL,
        reference_number TEXT PRIMARY KEY,
        gateway_transaction_id TEXT,
        failure_reason TEXT,

        CONSTRAINT shipment_foreign_key
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE CASCADE,
        CONSTRAINT unique_payment_per_shipment
                    UNIQUE (shipment_id),

        CONSTRAINT user_user_fk
                    FOREIGN KEY (customer_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE
        );

         CREATE INDEX idx_payments_status
         ON payments(status);

         CREATE INDEX idx_payments_payment_method
         ON payments(payment_method);

         -- !Downs

         DROP INDEX IF EXISTS  idx_payments_status;
         DROP INDEX IF EXISTS  idx_payments_payment_method;
         DROP TABLE IF EXISTS payments;

