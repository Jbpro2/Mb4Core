class ApkDownloadModal {
    __html = `
    <div class="modal-dialog modal-md">
        <div class="modal-content bg-dark text-white border-secondary">
            <div class="modal-header border-secondary">
                <h1 class="modal-title fs-5" id="exampleModalToggleLabel">BAIXAR APLICATIVO</h1>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 text-center">
                <i class="bi bi-android2 text-success" style="font-size: 4rem;"></i>
                <h4 class="mt-3">Gerar seu APK DTunnel</h4>
                <p class="text-muted">Clique no botão abaixo para compilar o aplicativo com suas configurações atuais.</p>
                
                <div id="apk-status" class="mt-4 d-none">
                    <div class="spinner-border text-primary" role="status"></div>
                    <p class="mt-2" id="status-text">Compilando APK... Isso pode levar de 5 a 10 minutos.</p>
                    <p class="small text-warning">Por favor, não feche esta janela.</p>
                </div>

                <div id="apk-actions" class="mt-4">
                    <button class="btn btn-success btn-lg w-100 mb-2" id="btn-generate-apk">
                        <i class="bi bi-gear-fill"></i> GERAR NOVO APK
                    </button>
                    <button class="btn btn-outline-primary btn-lg w-100 d-none" id="btn-download-apk">
                        <i class="bi bi-download"></i> BAIXAR APK GERADO
                    </button>
                </div>
            </div>
        </div>
    </div>`

    constructor() {
        this._element = document.createElement('div');
        this._element.classList.add('modal', 'fade');
        this._element.setAttribute('tabindex', '-1');
        this._element.innerHTML = this.__html;

        this.modal = new bootstrap.Modal(this._element);

        this._element.querySelector('#btn-generate-apk').addEventListener('click', () => this.generateApk());
        this._element.querySelector('#btn-download-apk').addEventListener('click', () => {
            window.location.href = '/api/app/download-apk';
        });
    }

    async generateApk() {
        const statusDiv = this._element.querySelector('#apk-status');
        const generateBtn = this._element.querySelector('#btn-generate-apk');
        const downloadBtn = this._element.querySelector('#btn-download-apk');
        const statusText = this._element.querySelector('#status-text');
        const csrfToken = typeof getCsrfTokenHead === 'function' ? getCsrfTokenHead() : '';

        statusDiv.classList.remove('d-none');
        generateBtn.disabled = true;
        downloadBtn.classList.add('d-none');

        try {
            // Aumentamos o tempo de espera no fetch para 15 minutos
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 900000);

            const response = await fetch('/api/app/generate-apk', {
                method: 'POST',
                headers: {
                    'csrf-token': csrfToken
                },
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (response.ok) {
                Swal.fire('Sucesso!', 'APK gerado com sucesso!', 'success');
                downloadBtn.classList.remove('d-none');
            } else {
                const result = await response.json();
                Swal.fire('Atenção', 'A compilação foi iniciada. Se o erro persistir, aguarde 5 minutos e tente baixar o APK diretamente.', 'info');
                downloadBtn.classList.remove('d-none');
            }
        } catch (error) {
            console.error(error);
            if (error.name === 'AbortError') {
                Swal.fire('Processando', 'A compilação está demorando mais que o esperado, mas continua rodando no servidor. Aguarde alguns minutos e clique em baixar.', 'info');
            } else {
                Swal.fire('Aviso', 'O servidor está processando seu APK. Aguarde 5 minutos e tente clicar no botão de baixar.', 'info');
            }
            downloadBtn.classList.remove('d-none');
        } finally {
            statusDiv.classList.add('d-none');
            generateBtn.disabled = false;
        }
    }

    show() {
        this.modal.show();
    }

    hide() {
        this.modal.hide();
    }
}

export default ApkDownloadModal;
