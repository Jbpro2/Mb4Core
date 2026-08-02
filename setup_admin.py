import sqlite3
import bcrypt
import uuid
import os
import sys

# Tentar encontrar o banco de dados no local padrão ou no local atual
db_path = '/opt/Mb4Core/prisma/database.db'
if not os.path.exists('/opt/Mb4Core'):
    db_path = 'prisma/database.db'

os.makedirs(os.path.dirname(db_path), exist_ok=True)

# Dados do Admin solicitados pelo usuário
username = 'Bk2026@12'
password = '2012@bk2520'
email = 'admin@dtunnel.com'

# Gerar hash da senha
hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(10)).decode('utf-8')

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Garantir que a tabela existe (mesmo que o prisma já tenha rodado)
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

# Inserir ou atualizar admin
try:
    user_id = str(uuid.uuid4())
    # Verificar se o usuário já existe pelo username
    cursor.execute('SELECT id FROM users WHERE username = ?', (username,))
    row = cursor.fetchone()
    
    if row:
        cursor.execute('UPDATE users SET password = ?, email = ? WHERE username = ?', (hashed_password, email, username))
        print(f"Usuário {username} atualizado com sucesso!")
    else:
        cursor.execute('''
        INSERT INTO users (id, username, password, email)
        VALUES (?, ?, ?, ?)
        ''', (user_id, username, hashed_password, email))
        print(f"Usuário {username} criado com sucesso!")
        
    conn.commit()
except Exception as e:
    print(f"Erro ao configurar admin: {e}")
finally:
    conn.close()
