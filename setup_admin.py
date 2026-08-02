import sqlite3
import bcrypt
import uuid
import os

db_path = 'prisma/database.db'
os.makedirs('prisma', exist_ok=True)

# Dados do Admin
username = 'Bk2026@12'
password = '2012@bk2520'
email = 'admin@dtunnel.com'

# Gerar hash da senha (usando salt rounds 10 como no projeto)
hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(10)).decode('utf-8')

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Criar tabela users se não existir (baseado no schema.prisma)
cursor.execute('''
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE,
    password TEXT,
    email TEXT UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    app_text_version INTEGER DEFAULT 1,
    app_layout_version INTEGER DEFAULT 1,
    app_config_version INTEGER DEFAULT 1
)
''')

# Inserir admin
try:
    user_id = str(uuid.uuid4())
    cursor.execute('''
    INSERT INTO users (id, username, password, email)
    VALUES (?, ?, ?, ?)
    ''', (user_id, username, hashed_password, email))
    conn.commit()
    print(f"Usuário {username} criado com sucesso!")
except sqlite3.IntegrityError:
    print(f"Usuário {username} já existe.")

conn.close()
