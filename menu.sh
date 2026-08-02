#!/bin/bash

# Cores
VERDE='\e[32m'
CIANO='\e[36m'
AMARELO='\e[33m'
VERMELHO='\e[31m'
RESET='\e[0m'

# Caminho do projeto
PROJETO_DIR="/opt/Mb4Core"
ENV_FILE="$PROJETO_DIR/.env"
DB_FILE="$PROJETO_DIR/prisma/database.db"

# Exportar variáveis do Android SDK
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Função para ler a porta atual
get_port() {
    if [ -f "$ENV_FILE" ]; then
        PORT=$(grep "^PORT=" "$ENV_FILE" | cut -d'=' -f2 | tr -d '"' | tr -d "'")
        echo "${PORT:-3000}"
    else
        echo "3000"
    fi
}

# Função para Backup
fazer_backup() {
    echo -e "\n${AMARELO}Iniciando backup...${RESET}"
    BACKUP_NAME="backup_$(date +%Y%m%d_%H%M%S).zip"
    zip -r "$BACKUP_NAME" "$DB_FILE" "$ENV_FILE" > /dev/null
    echo -e "${VERDE}Backup concluído: $BACKUP_NAME${RESET}"
    sleep 2
}

# Função para Restaurar
restaurar_backup() {
    echo -ne "\n${AMARELO}Digite o nome do arquivo de backup (ex: backup.zip): ${RESET}"
    read arquivo
    if [ -f "$arquivo" ]; then
        unzip -o "$arquivo" -d / > /dev/null
        echo -e "${VERDE}Backup restaurado com sucesso!${RESET}"
    else
        echo -e "${VERMELHO}Arquivo não encontrado!${RESET}"
    fi
    sleep 2
}

# Função para Reiniciar
reiniciar_painel() {
    echo -e "\n${AMARELO}Reiniciando painel...${RESET}"
    cd "$PROJETO_DIR"
    pm2 delete DTunnel 2>/dev/null
    pm2 start ecosystem.config.js
    pm2 save
    echo -e "${VERDE}Painel reiniciado com sucesso!${RESET}"
    sleep 2
}

# Função para Configurar Porta
configurar_porta() {
    echo -ne "\n${AMARELO}Digite a nova porta (Atual: $(get_port)): ${RESET}"
    read nova_porta
    if [[ "$nova_porta" =~ ^[0-9]+$ ]]; then
        if [ -f "$ENV_FILE" ]; then
            sed -i "s/^PORT=.*/PORT=$nova_porta/" "$ENV_FILE"
        else
            echo "PORT=$nova_porta" > "$ENV_FILE"
        fi
        echo -e "${VERDE}Porta alterada para $nova_porta.${RESET}"
        echo -e "${AMARELO}Reiniciando o painel para aplicar a alteração...${RESET}"
        reiniciar_painel
    else
        echo -e "${VERMELHO}Porta inválida!${RESET}"
        sleep 2
    fi
}

# Função para Gerar APK
gerar_apk() {
    echo -e "\n${AMARELO}Iniciando compilação do APK...${RESET}"
    echo -e "${CIANO}Isso pode demorar alguns minutos. Aguarde...${RESET}"
    
    cd "$PROJETO_DIR"
    
    if [ ! -d "/opt/android-sdk" ]; then
        echo -e "${VERMELHO}Android SDK não encontrado! Reinstale usando o ssh-plus.${RESET}"
        sleep 3
        return
    fi

    chmod +x gradlew
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        APK_PATH="$PROJETO_DIR/app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK_PATH" ]; then
            cp "$APK_PATH" "$PROJETO_DIR/DTunnelMod.apk"
            echo -e "${VERDE}APK gerado com sucesso!${RESET}"
            echo -e "Arquivo salvo em: ${CIANO}/opt/Mb4Core/DTunnelMod.apk${RESET}"
        else
            echo -e "${VERMELHO}Erro: APK não encontrado após a compilação.${RESET}"
        fi
    else
        echo -e "${VERMELHO}Erro ao compilar o APK. Verifique a memória do servidor.${RESET}"
    fi
    sleep 3
}

# Menu Principal
while true; do
    clear
    PORTA_ATUAL=$(get_port)
    echo -e "${VERDE}┌──────────────────────────────────┐${RESET}"
    echo -e "${VERDE}│      SERVER - VPS - TESTE        │${RESET}"
    echo -e "${VERDE}└──────────────────────────────────┘${RESET}"
    echo ""
    echo -e "${VERDE}====================================${RESET}"
    echo -e "${VERDE}      BEM VINDO AO DTUNNEL MOD      ${RESET}"
    echo -e "${VERDE}====================================${RESET}"
    echo ""
    echo -e "${CIANO}1.${RESET} Fazer backup"
    echo -e "${CIANO}2.${RESET} Restaurar backup"
    echo -e "${CIANO}3.${RESET} Reiniciar painel"
    echo -e "${CIANO}4.${RESET} Ativar Auto backup ${AMARELO}(Em breve)${RESET}"
    echo -e "${CIANO}5.${RESET} Remover painel"
    echo -e "${CIANO}6.${RESET} Gerar Aplicativo (APK)"
    echo -e "${CIANO}7.${RESET} Configurar porta do painel ${AMARELO}(Atual: $PORTA_ATUAL)${RESET}"
    echo -e "${CIANO}0.${RESET} Sair"
    echo ""
    echo -ne "${VERDE}Escolha uma opcao: ${RESET}"
    read opcao

    case $opcao in
        1) fazer_backup ;;
        2) restaurar_backup ;;
        3) reiniciar_painel ;;
        4) echo -e "\nFuncionalidade em desenvolvimento..."; sleep 2 ;;
        5) pm2 delete DTunnel; echo -e "${VERMELHO}Painel removido!${RESET}"; sleep 2 ;;
        6) gerar_apk ;;
        7) configurar_porta ;;
        0) exit 0 ;;
        *) echo -e "\nOpção inválida!"; sleep 1 ;;
    esac
done
