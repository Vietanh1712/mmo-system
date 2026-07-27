-- Bind each preorder to the exact product variant that must be fulfilled.
IF COL_LENGTH('PreOrders', 'variant_id') IS NULL
BEGIN
    ALTER TABLE PreOrders ADD variant_id BIGINT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_PreOrder_Variant'
)
BEGIN
    ALTER TABLE PreOrders
        ADD CONSTRAINT FK_PreOrder_Variant
        FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION;
END;
GO

-- Safely backfill legacy preorders only when the product has exactly one
-- active variant. Multi-variant products remain NULL for manual review.
UPDATE po
SET po.variant_id = single_variant.variant_id
FROM PreOrders po
INNER JOIN (
    SELECT product_id, MIN(id) AS variant_id
    FROM ProductVariants
    WHERE isDelete = 0
    GROUP BY product_id
    HAVING COUNT(*) = 1
) single_variant ON single_variant.product_id = po.product_id
WHERE po.variant_id IS NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_PreOrders_Variant_Status'
      AND object_id = OBJECT_ID('PreOrders')
)
BEGIN
    CREATE INDEX IX_PreOrders_Variant_Status
        ON PreOrders(variant_id, status, created_at);
END;
GO
