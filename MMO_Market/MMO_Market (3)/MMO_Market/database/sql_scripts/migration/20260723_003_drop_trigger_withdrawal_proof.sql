-- Script để xoá bỏ trigger tự động cập nhật file chứng từ rút tiền.
-- Nguyên nhân xoá: Trigger này tự động ghi đè proof_file bằng 'proof_bank_' + id mỗi khi hoàn tất rút tiền,
-- làm hỏng đường dẫn file ảnh thật (UUID) được backend tải lên.
DROP TRIGGER IF EXISTS trg_UpdateWithdrawalProof;
GO
