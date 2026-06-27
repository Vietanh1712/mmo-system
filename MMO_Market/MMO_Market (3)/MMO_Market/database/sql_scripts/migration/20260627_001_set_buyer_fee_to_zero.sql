-- Migration: Set FLAT_BUYER_FEE_VND to 0 to disable buyer service fee
-- Rollback: UPDATE SystemConfigurations SET config_value = '1000' WHERE config_key = 'FLAT_BUYER_FEE_VND';

UPDATE SystemConfigurations 
SET config_value = '0' 
WHERE config_key = 'FLAT_BUYER_FEE_VND';
