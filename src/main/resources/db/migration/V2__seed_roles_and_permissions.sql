-- ── Roles ─────────────────────────────────────────────────────────────────────
INSERT INTO roles (name) VALUES ('USER')  ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- ── Permissions ───────────────────────────────────────────────────────────────
INSERT INTO permissions (name) VALUES ('users:read')   ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name) VALUES ('users:write')  ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name) VALUES ('users:delete') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name) VALUES ('admin:access') ON CONFLICT (name) DO NOTHING;

-- ── Role → Permission atamaları ───────────────────────────────────────────────

-- USER rolü: sadece okuma
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN ('users:read')
ON CONFLICT DO NOTHING;

-- ADMIN rolü: tüm yetkiler
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
