INSERT INTO companies (id, name, rut, created_at)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'Dev Company', '76.000.000-0', now())
ON CONFLICT (id) DO NOTHING;