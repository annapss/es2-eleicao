-- =====================================================================
-- Sistema de Simulação de Eleição Eletrônica
-- Modelo de banco de dados (SQLite 3.25 ou superior)
--
-- IMPORTANTE: o SQLite NÃO aplica chaves estrangeiras por padrão.
-- A linha abaixo vale só para a conexão que executa este script.
-- A API deve executar "PRAGMA foreign_keys = ON;" em TODA conexão que abrir.
-- (SQLite 3.25+ é necessário para a função RANK() usada na view de resultado.)
-- =====================================================================

PRAGMA foreign_keys = ON;

-- Convenções do SQLite usadas neste script:
--   * datas e horas são guardadas como TEXT: data 'AAAA-MM-DD', hora 'HH:MM',
--     data e hora completas 'AAAA-MM-DD HH:MM:SS' (CURRENT_TIMESTAMP, em UTC);
--   * booleanos são INTEGER (0 ou 1).

-- ---------------------------------------------------------------------
-- 1. TABELAS DE APOIO
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS cargo (
    id    INTEGER PRIMARY KEY,
    nome  TEXT    NOT NULL UNIQUE,
    ordem INTEGER NOT NULL UNIQUE         -- ordem de exibição nas telas e relatórios
);

INSERT OR IGNORE INTO cargo (id, nome, ordem) VALUES
    (1, 'Deputado Estadual',         1),
    (2, 'Deputado Federal',          2),
    (3, 'Senador',                   3),
    (4, 'Governador',                4),
    (5, 'Presidente da República',   5);

CREATE TABLE IF NOT EXISTS partido (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    sigla TEXT    NOT NULL UNIQUE,
    nome  TEXT    NOT NULL
);

-- ---------------------------------------------------------------------
-- 2. USUÁRIOS E PERFIS
--    usuario guarda login e perfil (Chefe da Seção, Eleitor ou Candidato).
--    eleitor e candidato guardam os dados específicos de cada perfil.
--    O Chefe da Seção não precisa de tabela própria.
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS usuario (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    login      TEXT    NOT NULL UNIQUE,
    senha_hash TEXT    NOT NULL,                      -- nunca guardar senha em texto puro
    perfil     TEXT    NOT NULL
               CHECK (perfil IN ('CHEFE_SECAO', 'ELEITOR', 'CANDIDATO')),
    ativo      INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
    criado_em  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, perfil)                                -- permite o vínculo seguro com eleitor/candidato
);

CREATE TABLE IF NOT EXISTS eleitor (
    usuario_id        INTEGER PRIMARY KEY,
    perfil            TEXT    NOT NULL DEFAULT 'ELEITOR' CHECK (perfil = 'ELEITOR'),
    nome              TEXT    NOT NULL,
    cpf               TEXT    NOT NULL UNIQUE CHECK (length(cpf) = 11),
    titulo_eleitor    TEXT    NOT NULL UNIQUE,
    data_nascimento   TEXT    NOT NULL CHECK (date(data_nascimento) IS NOT NULL),
    -- garante que só um usuário com perfil ELEITOR vira eleitor
    FOREIGN KEY (usuario_id, perfil) REFERENCES usuario (id, perfil) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 3. ELEIÇÃO
--    Cada linha é uma eleição em uma data e turno. Estados possíveis:
--    AGENDADA -> ABERTA -> ENCERRADA (só o Chefe da Seção muda o estado).
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS eleicao (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    data_eleicao        TEXT    NOT NULL CHECK (date(data_eleicao) IS NOT NULL),   -- 'AAAA-MM-DD'
    turno               INTEGER NOT NULL CHECK (turno IN (1, 2)),
    hora_abertura       TEXT    NOT NULL CHECK (time(hora_abertura) IS NOT NULL),  -- 'HH:MM'
    hora_encerramento   TEXT    NOT NULL CHECK (time(hora_encerramento) IS NOT NULL),
    status              TEXT    NOT NULL DEFAULT 'AGENDADA'
                        CHECK (status IN ('AGENDADA', 'ABERTA', 'ENCERRADA')),
    aberta_em           TEXT,                              -- momento real em que o chefe abriu
    encerrada_em        TEXT,                              -- momento real em que o chefe encerrou
    criada_por          INTEGER NOT NULL REFERENCES usuario (id),
    criada_em           TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_eleicao_data_turno UNIQUE (data_eleicao, turno),
    CONSTRAINT ck_eleicao_horario    CHECK (time(hora_encerramento) > time(hora_abertura)),
    CONSTRAINT ck_eleicao_status_datas CHECK (
            (status = 'AGENDADA'  AND aberta_em IS NULL     AND encerrada_em IS NULL)
        OR (status = 'ABERTA'    AND aberta_em IS NOT NULL AND encerrada_em IS NULL)
        OR (status = 'ENCERRADA' AND aberta_em IS NOT NULL AND encerrada_em IS NOT NULL)
    )
);

-- ---------------------------------------------------------------------
-- 4. CANDIDATO
--    Cada linha é uma candidatura: uma pessoa (usuario) concorrendo a um
--    cargo em uma eleição, com número e partido.
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS candidato (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id  INTEGER NOT NULL,
    perfil      TEXT    NOT NULL DEFAULT 'CANDIDATO' CHECK (perfil = 'CANDIDATO'),
    nome        TEXT    NOT NULL,
    numero      INTEGER NOT NULL CHECK (numero > 0),
    partido_id  INTEGER NOT NULL REFERENCES partido (id),
    cargo_id    INTEGER NOT NULL REFERENCES cargo (id),
    eleicao_id  INTEGER NOT NULL REFERENCES eleicao (id),

    FOREIGN KEY (usuario_id, perfil) REFERENCES usuario (id, perfil) ON DELETE CASCADE,

    -- o número é único dentro do mesmo cargo e da mesma eleição
    CONSTRAINT uq_candidato_numero UNIQUE (eleicao_id, cargo_id, numero),
    -- a mesma pessoa só concorre a um cargo por eleição
    CONSTRAINT uq_candidato_usuario_eleicao UNIQUE (usuario_id, eleicao_id),
    -- usada pelas chaves estrangeiras de voto, para garantir que o candidato
    -- pertence à eleição e ao cargo informados no voto
    CONSTRAINT uq_candidato_id_eleicao_cargo UNIQUE (id, eleicao_id, cargo_id)
);

CREATE INDEX IF NOT EXISTS ix_candidato_eleicao_cargo ON candidato (eleicao_id, cargo_id);

-- ---------------------------------------------------------------------
-- 5. VOTAÇÃO COM SIGILO
--
--  O sigilo vem da separação em três tabelas:
--
--  voto_pendente : voto registrado, ainda não confirmado. Tem o eleitor,
--                  porque ele precisa poder confirmar. É APAGADA na confirmação.
--  comparecimento: QUEM já votou em qual cargo (impede voto duplicado).
--                  Não guarda o candidato.
--  voto          : o voto confirmado. Guarda o candidato, mas NÃO guarda
--                  o eleitor, nem data e hora, nem ID sequencial.
--
--  Não existe nenhuma coluna que ligue comparecimento a voto.
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS voto_pendente (
    eleitor_id    INTEGER NOT NULL REFERENCES eleitor (usuario_id) ON DELETE CASCADE,
    eleicao_id    INTEGER NOT NULL,
    cargo_id      INTEGER NOT NULL,
    candidato_id  INTEGER NOT NULL,
    registrado_em TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eleitor_id, eleicao_id, cargo_id),    -- um voto pendente por cargo
    FOREIGN KEY (candidato_id, eleicao_id, cargo_id)
        REFERENCES candidato (id, eleicao_id, cargo_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comparecimento (
    eleitor_id INTEGER NOT NULL REFERENCES eleitor (usuario_id),   -- sem CASCADE: quem votou não pode ser excluído
    eleicao_id INTEGER NOT NULL REFERENCES eleicao (id),
    cargo_id   INTEGER NOT NULL REFERENCES cargo (id),
    votou_em   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eleitor_id, eleicao_id, cargo_id)     -- impede mais de um voto no mesmo cargo
);

CREATE TABLE IF NOT EXISTS voto (
    -- identificador aleatório (32 caracteres hexadecimais), para não revelar a ordem dos votos
    id           TEXT    PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    eleicao_id   INTEGER NOT NULL,
    cargo_id     INTEGER NOT NULL,
    candidato_id INTEGER NOT NULL,
    -- sem CASCADE: candidato que já recebeu voto não pode ser excluído
    FOREIGN KEY (candidato_id, eleicao_id, cargo_id)
        REFERENCES candidato (id, eleicao_id, cargo_id)
);

CREATE INDEX IF NOT EXISTS ix_voto_apuracao ON voto (eleicao_id, cargo_id, candidato_id);

-- ---------------------------------------------------------------------
-- 6. REGRAS NO BANCO (última barreira; a API também valida)
-- ---------------------------------------------------------------------

-- Só aceita voto enquanto a eleição está ABERTA
-- CREATE TRIGGER trg_voto_eleicao_aberta
-- BEFORE INSERT ON voto
-- WHEN NOT EXISTS (SELECT 1 FROM eleicao WHERE id = NEW.eleicao_id AND status = 'ABERTA')
-- BEGIN
--     SELECT RAISE(ABORT, 'A eleição não está ABERTA');
-- END;

-- CREATE TRIGGER trg_comparecimento_eleicao_aberta
-- BEFORE INSERT ON comparecimento
-- WHEN NOT EXISTS (SELECT 1 FROM eleicao WHERE id = NEW.eleicao_id AND status = 'ABERTA')
-- BEGIN
--     SELECT RAISE(ABORT, 'A eleição não está ABERTA');
-- END;

-- CREATE TRIGGER trg_voto_pendente_eleicao_aberta
-- BEFORE INSERT ON voto_pendente
-- WHEN NOT EXISTS (SELECT 1 FROM eleicao WHERE id = NEW.eleicao_id AND status = 'ABERTA')
-- BEGIN
--     SELECT RAISE(ABORT, 'A eleição não está ABERTA');
-- END;

-- Voto confirmado não pode ser alterado nem removido
-- CREATE TRIGGER trg_voto_imutavel_update
-- BEFORE UPDATE ON voto
-- BEGIN
--     SELECT RAISE(ABORT, 'Voto confirmado não pode ser alterado nem removido');
-- END;

-- CREATE TRIGGER trg_voto_imutavel_delete
-- BEFORE DELETE ON voto
-- BEGIN
--     SELECT RAISE(ABORT, 'Voto confirmado não pode ser alterado nem removido');
-- END;

-- ---------------------------------------------------------------------
-- 7. VIEWS DE APURAÇÃO
-- ---------------------------------------------------------------------

-- Placar parcial ou final: votos por candidato (candidatos sem voto aparecem com 0).
-- Filtrar por eleicao_id (e por cargo_id para o perfil Candidato).
CREATE VIEW IF NOT EXISTS vw_apuracao AS
SELECT  c.eleicao_id,
        c.cargo_id,
        cg.nome       AS cargo,
        cg.ordem      AS cargo_ordem,
        c.id          AS candidato_id,
        c.numero,
        c.nome        AS candidato,
        p.sigla       AS partido,
        COUNT(v.id)   AS total_votos
FROM    candidato c
JOIN    cargo   cg ON cg.id = c.cargo_id
JOIN    partido p  ON p.id  = c.partido_id
LEFT JOIN voto  v  ON v.candidato_id = c.id
GROUP BY c.eleicao_id, c.cargo_id, cg.nome, cg.ordem, c.id, c.numero, c.nome, p.sigla;

-- Resultado final: só existe para eleições ENCERRADAS.
-- posicao = 1 é o mais votado. Em caso de empate, mais de um candidato tem posicao = 1
-- (a API decide como tratar o empate). mais_votado vale 1 ou 0.
CREATE VIEW IF NOT EXISTS vw_resultado_final AS
SELECT  r.*,
        (r.posicao = 1 AND r.total_votos > 0) AS mais_votado
FROM (
    SELECT  a.*,
            RANK() OVER (PARTITION BY a.eleicao_id, a.cargo_id
                         ORDER BY a.total_votos DESC) AS posicao
    FROM    vw_apuracao a
    JOIN    eleicao e ON e.id = a.eleicao_id
    WHERE   e.status = 'ENCERRADA'
) r;

-- ---------------------------------------------------------------------
-- 8. DADOS INICIAIS (exemplo; troque o hash por um gerado pela sua API)
-- ---------------------------------------------------------------------
-- INSERT INTO usuario (login, senha_hash, perfil)
-- VALUES ('chefe', '<hash gerado pela API>', 'CHEFE_SECAO');

-- ---------------------------------------------------------------------
-- 9. EXEMPLO DE CONFIRMAÇÃO DE VOTO (uma única transação, atômica)
--    Move o voto pendente para a urna sem deixar o vínculo com o eleitor.
--    O SQLite não permite DELETE ... RETURNING dentro de um WITH, então a
--    confirmação usa três comandos dentro de uma transação:
--
--    BEGIN;
--    INSERT INTO comparecimento (eleitor_id, eleicao_id, cargo_id)
--        SELECT eleitor_id, eleicao_id, cargo_id FROM voto_pendente
--        WHERE eleitor_id = ?1 AND eleicao_id = ?2 AND cargo_id = ?3;
--    INSERT INTO voto (eleicao_id, cargo_id, candidato_id)
--        SELECT eleicao_id, cargo_id, candidato_id FROM voto_pendente
--        WHERE eleitor_id = ?1 AND eleicao_id = ?2 AND cargo_id = ?3;
--    DELETE FROM voto_pendente
--        WHERE eleitor_id = ?1 AND eleicao_id = ?2 AND cargo_id = ?3;
--    COMMIT;
--
--    A API confere quantas linhas o primeiro INSERT afetou:
--      * 0 linhas -> não havia voto pendente: ROLLBACK e responder 404;
--      * se o eleitor já votou no cargo, a chave primária de comparecimento
--        falha: ROLLBACK e responder 409.
-- ---------------------------------------------------------------------