-- ============================================================
-- Sample Orders
-- ============================================================

INSERT INTO orders (
    order_id,
    customer_id,
    customer_name,
    product_code,
    quantity,
    unit_price,
    total_amount,
    status,
    priority,
    shipping_address,
    failure_reason,
    processed_at,
    created_at,
    updated_at
)
VALUES (
           'ORD-SAMPLE-001',
           'CUST-001',
           'John Doe',
           'LAPTOP-001',
           1,
           75000.00,
           75000.00,
           'PENDING',
           'NORMAL',
           'Bangalore, Karnataka',
           NULL,
           NULL,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       )
    ON CONFLICT (order_id) DO NOTHING;


INSERT INTO orders (
    order_id,
    customer_id,
    customer_name,
    product_code,
    quantity,
    unit_price,
    total_amount,
    status,
    priority,
    shipping_address,
    failure_reason,
    processed_at,
    created_at,
    updated_at
)
VALUES (
           'ORD-SAMPLE-002',
           'CUST-002',
           'Jane Smith',
           'PHONE-001',
           2,
           25000.00,
           50000.00,
           'PENDING',
           'HIGH',
           'Hyderabad, Telangana',
           NULL,
           NULL,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       )
    ON CONFLICT (order_id) DO NOTHING;


INSERT INTO orders (
    order_id,
    customer_id,
    customer_name,
    product_code,
    quantity,
    unit_price,
    total_amount,
    status,
    priority,
    shipping_address,
    failure_reason,
    processed_at,
    created_at,
    updated_at
)
VALUES (
           'ORD-SAMPLE-003',
           'CUST-003',
           'Robert Kumar',
           'HEADPHONE-001',
           1,
           5000.00,
           5000.00,
           'FULFILLED',
           'NORMAL',
           'Chennai, Tamil Nadu',
           NULL,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       )
    ON CONFLICT (order_id) DO NOTHING;