-- Utilisateurs de démo de la PoC (pas d'authentification).
-- UUID fixes pour que le back et les tests les retrouvent.
INSERT INTO user_account (id, first_name, last_name, role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'John', 'Doe', 'CUSTOMER'),
    ('00000000-0000-0000-0000-000000000002', 'Jean', 'Biche', 'AGENT');