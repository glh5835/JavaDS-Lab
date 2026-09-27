-- =====================================================================
-- JavaDS-Lab 持久化 Schema（SQLite）
-- 四张表：algorithms / runs / steps / mistakes
-- 设计要点：外键级联删除、CHECK 约束保证数据合法性、查询路径全覆盖索引
-- =====================================================================

PRAGMA foreign_keys = ON;

-- 算法/数据结构主表
CREATE TABLE IF NOT EXISTS algorithms (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,                 -- 算法名，如 "dijkstra"
    category    TEXT    NOT NULL CHECK (category IN
                  ('linear','stack_queue','hash','tree','graph','sort','search','dp','string')),
    difficulty  INTEGER NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    description TEXT
);

-- 一次运行记录（某算法的一次执行/一次练习）
CREATE TABLE IF NOT EXISTS runs (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    algorithm_id INTEGER NOT NULL,
    ran_at       TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    duration_ms  INTEGER NOT NULL CHECK (duration_ms >= 0),
    input_size   INTEGER NOT NULL CHECK (input_size >= 0),
    passed       INTEGER NOT NULL CHECK (passed IN (0, 1)),
    score        REAL    CHECK (score IS NULL OR score >= 0),
    FOREIGN KEY (algorithm_id) REFERENCES algorithms(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_runs_algo       ON runs(algorithm_id);
CREATE INDEX IF NOT EXISTS idx_runs_time       ON runs(ran_at);
CREATE INDEX IF NOT EXISTS idx_runs_algo_time  ON runs(algorithm_id, ran_at);
CREATE INDEX IF NOT EXISTS idx_runs_passed     ON runs(passed);

-- 运行的操作步骤（对应模块二的 trace step，用于回放审计）
CREATE TABLE IF NOT EXISTS steps (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id  INTEGER NOT NULL,
    step_no INTEGER NOT NULL CHECK (step_no >= 0),
    op      TEXT    NOT NULL,
    detail  TEXT,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_steps_run ON steps(run_id, step_no);

-- 错题本：做错的题、错误原因、重做次数
CREATE TABLE IF NOT EXISTS mistakes (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    algorithm_id INTEGER NOT NULL,
    question     TEXT    NOT NULL,
    wrong_answer TEXT    NOT NULL,
    reason       TEXT    NOT NULL,
    redo_count   INTEGER NOT NULL DEFAULT 0 CHECK (redo_count >= 0),
    created_at   TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (algorithm_id) REFERENCES algorithms(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mistakes_algo  ON mistakes(algorithm_id);
CREATE INDEX IF NOT EXISTS idx_mistakes_redo  ON mistakes(redo_count);
