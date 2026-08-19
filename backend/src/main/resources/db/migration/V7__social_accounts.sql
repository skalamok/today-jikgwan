-- REQ-F-003 소셜 로그인 (구글 · 네이버 · 카카오)
-- user_social_accounts 는 V1 에서 만들어 두었으나 실제 연동에 필요한 항목이 빠져 있었다.
ALTER TABLE user_social_accounts
    ADD COLUMN IF NOT EXISTS email      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 제공자가 이메일을 주지 않을 수 있어(카카오는 비즈앱 심사 대상) 식별에는 쓰지 않고 참고용으로만 둔다.
COMMENT ON COLUMN user_social_accounts.email IS '제공자가 알려준 이메일. 없을 수 있다';

-- 한 계정에 같은 제공자를 두 번 연결하지 않는다.
ALTER TABLE user_social_accounts
    ADD CONSTRAINT uk_social_user_provider UNIQUE (user_id, provider);

CREATE INDEX IF NOT EXISTS idx_social_user ON user_social_accounts (user_id);
