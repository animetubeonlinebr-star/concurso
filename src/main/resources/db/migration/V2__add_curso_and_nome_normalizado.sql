CREATE TABLE IF NOT EXISTS curso (
                                     id                BIGSERIAL    PRIMARY KEY,
                                     concurso_id       BIGINT       NOT NULL,
                                     nome              VARCHAR(255) NOT NULL,
    nome_normalizado  VARCHAR(255) NOT NULL,
    codigo            VARCHAR(50),
    ordem             INTEGER,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_curso_concurso
    FOREIGN KEY (concurso_id) REFERENCES concurso(id) ON DELETE CASCADE,
    CONSTRAINT uk_curso_concurso_nome_norm
    UNIQUE (concurso_id, nome_normalizado)
    );

CREATE INDEX IF NOT EXISTS idx_curso_concurso_id ON curso(concurso_id);


INSERT INTO curso (concurso_id, nome, nome_normalizado, codigo, ordem)
SELECT c.id, 'Geral', 'GERAL', 'GERAL', 1
FROM concurso c
WHERE NOT EXISTS (
    SELECT 1 FROM curso cu
    WHERE cu.concurso_id = c.id AND cu.nome_normalizado = 'GERAL'
);


ALTER TABLE materia ADD COLUMN IF NOT EXISTS curso_id         BIGINT;
ALTER TABLE materia ADD COLUMN IF NOT EXISTS nome_normalizado VARCHAR(255);

UPDATE materia m
SET curso_id = (
    SELECT cu.id FROM curso cu
    WHERE cu.concurso_id = m.concurso_id
      AND cu.nome_normalizado = 'GERAL'
    LIMIT 1
    )
WHERE m.curso_id IS NULL;

UPDATE materia
SET nome_normalizado = UPPER(TRIM(nome))
WHERE nome_normalizado IS NULL;

ALTER TABLE materia ALTER COLUMN curso_id         SET NOT NULL;
ALTER TABLE materia ALTER COLUMN nome_normalizado SET NOT NULL;


ALTER TABLE materia DROP CONSTRAINT IF EXISTS fk_materia_concurso;
ALTER TABLE materia DROP CONSTRAINT IF EXISTS uk_materia_curso_nome_norm;

ALTER TABLE materia
    ADD CONSTRAINT fk_materia_curso
        FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE CASCADE;
ALTER TABLE materia
    ADD CONSTRAINT uk_materia_curso_nome_norm
        UNIQUE (curso_id, nome_normalizado);

CREATE INDEX IF NOT EXISTS idx_materia_curso_id ON materia(curso_id);


ALTER TABLE topico ADD COLUMN IF NOT EXISTS nome_normalizado VARCHAR(255);

UPDATE topico
SET nome_normalizado = UPPER(TRIM(nome))
WHERE nome_normalizado IS NULL;

ALTER TABLE topico ALTER COLUMN nome_normalizado SET NOT NULL;

ALTER TABLE topico DROP CONSTRAINT IF EXISTS uk_topico_materia_nome_norm;
ALTER TABLE topico
    ADD CONSTRAINT uk_topico_materia_nome_norm
        UNIQUE (materia_id, nome_normalizado);
