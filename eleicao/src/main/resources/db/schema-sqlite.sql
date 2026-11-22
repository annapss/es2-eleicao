PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS cargo (
    id    INTEGER PRIMARY KEY,
    nome  TEXT    NOT NULL UNIQUE,
    ordem INTEGER NOT NULL UNIQUE
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

CREATE TABLE IF NOT EXISTS usuario (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    login      TEXT    NOT NULL UNIQUE,
    senha_hash TEXT    NOT NULL,
    perfil     TEXT    NOT NULL
               CHECK (perfil IN ('CHEFE_SECAO', 'ELEITOR', 'CANDIDATO')),
    ativo      INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
    criado_em  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, perfil)
);

CREATE TABLE IF NOT EXISTS eleitor (
    usuario_id        INTEGER PRIMARY KEY,
    perfil            TEXT    NOT NULL DEFAULT 'ELEITOR' CHECK (perfil = 'ELEITOR'),
    nome              TEXT    NOT NULL,
    cpf               TEXT    NOT NULL UNIQUE CHECK (length(cpf) = 11),
    titulo_eleitor    TEXT    NOT NULL UNIQUE,
    data_nascimento   TEXT    NOT NULL CHECK (date(data_nascimento) IS NOT NULL),
    FOREIGN KEY (usuario_id, perfil) REFERENCES usuario (id, perfil) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS eleicao (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    data_eleicao        TEXT    NOT NULL CHECK (date(data_eleicao) IS NOT NULL),
    turno               INTEGER NOT NULL CHECK (turno IN (1, 2)),
    hora_abertura       TEXT    NOT NULL CHECK (time(hora_abertura) IS NOT NULL),
    hora_encerramento   TEXT    NOT NULL CHECK (time(hora_encerramento) IS NOT NULL),
    status              TEXT    NOT NULL DEFAULT 'AGENDADA'
                        CHECK (status IN ('AGENDADA', 'ABERTA', 'ENCERRADA')),
    aberta_em           TEXT,
    encerrada_em        TEXT,
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

CREATE TABLE IF NOT EXISTS candidato (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id  INTEGER NOT NULL,
    perfil      TEXT    NOT NULL DEFAULT 'CANDIDATO' CHECK (perfil = 'CANDIDATO'),
    nome        TEXT    NOT NULL,
    data_nascimento TEXT NOT NULL CHECK (date(data_nascimento) IS NOT NULL),
    numero      INTEGER NOT NULL CHECK (numero > 0),
    partido_id  INTEGER NOT NULL REFERENCES partido (id),
    cargo_id    INTEGER NOT NULL REFERENCES cargo (id),
    eleicao_id  INTEGER NOT NULL REFERENCES eleicao (id),

    FOREIGN KEY (usuario_id, perfil) REFERENCES usuario (id, perfil) ON DELETE CASCADE,

    CONSTRAINT uq_candidato_numero UNIQUE (eleicao_id, cargo_id, numero),
    CONSTRAINT uq_candidato_usuario_eleicao UNIQUE (usuario_id, eleicao_id),
    CONSTRAINT uq_candidato_id_eleicao_cargo UNIQUE (id, eleicao_id, cargo_id)
);

CREATE INDEX IF NOT EXISTS ix_candidato_eleicao_cargo ON candidato (eleicao_id, cargo_id);

CREATE TABLE IF NOT EXISTS voto_pendente (
    eleitor_id    INTEGER NOT NULL REFERENCES eleitor (usuario_id) ON DELETE CASCADE,
    eleicao_id    INTEGER NOT NULL,
    cargo_id      INTEGER NOT NULL,
    candidato_id  INTEGER NOT NULL,
    registrado_em TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eleitor_id, eleicao_id, cargo_id),
    FOREIGN KEY (candidato_id, eleicao_id, cargo_id)
        REFERENCES candidato (id, eleicao_id, cargo_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comparecimento (
    eleitor_id INTEGER NOT NULL REFERENCES eleitor (usuario_id),
    eleicao_id INTEGER NOT NULL REFERENCES eleicao (id),
    cargo_id   INTEGER NOT NULL REFERENCES cargo (id),
    votou_em   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (eleitor_id, eleicao_id, cargo_id)
);

CREATE TABLE IF NOT EXISTS voto (
    id           TEXT    PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    eleicao_id   INTEGER NOT NULL,
    cargo_id     INTEGER NOT NULL,
    candidato_id INTEGER NOT NULL,
    FOREIGN KEY (candidato_id, eleicao_id, cargo_id)
        REFERENCES candidato (id, eleicao_id, cargo_id)
);

CREATE INDEX IF NOT EXISTS ix_voto_apuracao ON voto (eleicao_id, cargo_id, candidato_id);

CREATE VIEW IF NOT EXISTS vw_apuracao AS
SELECT  c.eleicao_id,
        c.cargo_id,
        cg.nome       AS cargo,
        cg.ordem      AS cargo_ordem,
        c.id          AS candidato_id,
        c.numero,
        c.nome        AS candidato,
        c.data_nascimento AS data_nascimento,
        p.sigla       AS partido,
        COUNT(v.id)   AS total_votos
FROM    candidato c
JOIN    cargo   cg ON cg.id = c.cargo_id
JOIN    partido p  ON p.id  = c.partido_id
LEFT JOIN voto  v  ON v.candidato_id = c.id
GROUP BY c.eleicao_id, c.cargo_id, cg.nome, cg.ordem, c.id, c.numero, c.nome, c.data_nascimento, p.sigla;
