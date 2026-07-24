-- ATENÇÃO: dados de desenvolvimento/demo, não de produção.
-- Em um rollout real, isso não iria como migration numerada versionada junto do schema;
-- ficaria em um script de bootstrap separado (ou um profile "dev" com outra pasta de migrations).
-- Mantemos aqui por simplicidade neste estágio do projeto.
--
-- Senha de todos os usuários abaixo: "senha123" (hash BCrypt real, gerado com bcrypt de verdade).

INSERT INTO usuario (nome, email, senha, perfil) VALUES
    ('Ana Operadora',   'ana.operadora@trigodourado.com.br',   '$2b$10$iKHBk/APrDJtFBNFF4bePOF5xAgmcT6KmKYwcYSz7zB/GPZw0h5RO', 'OPERADOR'),
    ('Bruno Técnico',   'bruno.tecnico@trigodourado.com.br',   '$2b$10$iKHBk/APrDJtFBNFF4bePOF5xAgmcT6KmKYwcYSz7zB/GPZw0h5RO', 'TECNICO'),
    ('Carla Supervisora','carla.supervisora@trigodourado.com.br','$2b$10$iKHBk/APrDJtFBNFF4bePOF5xAgmcT6KmKYwcYSz7zB/GPZw0h5RO', 'SUPERVISOR'),
    ('Diego Gestor PCM','diego.gestor@trigodourado.com.br',     '$2b$10$iKHBk/APrDJtFBNFF4bePOF5xAgmcT6KmKYwcYSz7zB/GPZw0h5RO', 'GESTOR'),
    ('Elisa Gerente',   'elisa.gerente@trigodourado.com.br',    '$2b$10$iKHBk/APrDJtFBNFF4bePOF5xAgmcT6KmKYwcYSz7zB/GPZw0h5RO', 'GERENTE');

INSERT INTO linha_producao (id, nome, descricao) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Linha 1', 'Linha principal de biscoitos recheados');

INSERT INTO equipamento (linha_producao_id, codigo, nome, setor, status) VALUES
    ('11111111-1111-1111-1111-111111111111', 'MIST-01', 'Misturadora de Massa 01', 'Mistura', 'ATIVO'),
    ('11111111-1111-1111-1111-111111111111', 'FORNO-01', 'Forno Túnel 01', 'Forneamento', 'ATIVO'),
    ('11111111-1111-1111-1111-111111111111', 'EMPAC-01', 'Empacotadora 01', 'Embalagem', 'ATIVO');
