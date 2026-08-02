import sqlite3
import bcrypt
import uuid
import os

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

# Garantir que a tabela existe com as novas colunas
cursor.execute('''
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE,
    password TEXT,
    email TEXT UNIQUE,
    role TEXT DEFAULT 'user',
    expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    app_text_version INTEGER DEFAULT 1,
    app_layout_version INTEGER DEFAULT 1,
    app_config_version INTEGER DEFAULT 1
)
''')

# Tentar adicionar colunas caso a tabela já exista sem elas
try:
    cursor.execute('ALTER TABLE users ADD COLUMN role TEXT DEFAULT "user"')
except:
    pass
try:
    cursor.execute('ALTER TABLE users ADD COLUMN expires_at DATETIME')
except:
    pass

# Inserir ou atualizar admin
try:
    user_id = str(uuid.uuid4())
    cursor.execute('SELECT id FROM users WHERE username = ?', (username,))
    row = cursor.fetchone()
    
    if row:
        cursor.execute('UPDATE users SET password = ?, email = ?, role = ? WHERE username = ?', (hashed_password, email, 'admin', username))
        print(f"Usuário {username} atualizado como ADMIN com sucesso!")
    else:
        cursor.execute('''
        INSERT INTO users (id, username, password, email, role)
        VALUES (?, ?, ?, ?, ?)
        ''', (user_id, username, hashed_password, email, 'admin'))
        print(f"Usuário {username} criado como ADMIN com sucesso!")
        
    conn.commit()
except Exception as e:
    print(f"Erro ao configurar admin: {e}")
finally:
    conn.close()
