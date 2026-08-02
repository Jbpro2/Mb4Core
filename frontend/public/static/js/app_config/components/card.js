import { ComponentStyled } from "./core/base.js";

class CardDefault extends ComponentStyled {
    __html__ = `
        <div class="d-flex flex-column justify-content-around h-100 w-100 shadow rounded p-2">
            <div class="px-0 btn btn-dark __create border-0 w-100 d-flex align-items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="45" height="45" fill="currentColor" viewBox="0 0 16 16">
                    <path
                        d="M8.5 6a.5.5 0 0 0-1 0v1.5H6a.5.5 0 0 0 0 1h1.5V10a.5.5 0 0 0 1 0V8.5H10a.5.5 0 0 0 0-1H8.5V6z" />
                    <path
                        d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2zm10-1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1z" />
                </svg>
                <h5>CRIAR TEMA</h5>
            </div>
            <div class="px-0 btn btn-dark __import border-0 w-100 d-flex align-items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="45" height="45" fill="currentColor" viewBox="0 0 16 16">
                    <path
                        d="M8 5a.5.5 0 0 1 .5.5V7H10a.5.5 0 0 1 0 1H8.5v1.5a.5.5 0 0 1-1 0V8H6a.5.5 0 0 1 0-1h1.5V5.5A.5.5 0 0 1 8 5zm-2.5 6.5A.5.5 0 0 1 6 11h4a.5.5 0 0 1 0 1H6a.5.5 0 0 1-.5-.5z" />
                    <path
                        d="M14 14V4.5L9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2zM9.5 3A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5v2z" />
                </svg>
                <h5>IMPORTAR TEMA</h5>
            </div>
            <div class="px-1 btn btn-dark __sync border-0 w-100 d-flex align-items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="45" height="45" fill="currentColor" viewBox="0 0 16 16">
                    <path d="M11.534 7h3.932a.25.25 0 0 1 .192.41l-1.966 2.36a.25.25 0 0 1-.384 0l-1.966-2.36a.25.25 0 0 1 .192-.41zm-11 2h3.932a.25.25 0 0 0 .192-.41L2.692 6.23a.25.25 0 0 0-.384 0L.342 8.59A.25.25 0 0 0 .534 9z" />
                    <path d="M8 3c-1.552 0-2.94.707-3.857 1.818a.5.5 0 1 1-.771-.636A6.002 6.002 0 0 1 13.917 7H12.9A5.002 5.002 0 0 0 8 3zM3.1 9a5.002 5.002 0 0 0 8.757 2.182.5.5 0 1 1 .771.636A6.002 6.002 0 0 1 2.083 9H3.1z" />
                </svg>
                <h5>SINCRONIZAR</h5>
            </div>
            <div class="px-1 btn btn-dark __apk_download border-0 w-100 d-flex align-items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="45" height="45" fill="currentColor" class="bi bi-android2" viewBox="0 0 16 16">
                    <path d="m10.213 1.471.691-1.26c.046-.083.03-.147-.048-.192-.085-.038-.15-.019-.195.058l-.7 1.27A4.832 4.832 0 0 0 8.005 1a4.816 4.816 0 0 0-1.962.418l-.7-1.27c-.045-.076-.11-.095-.195-.058-.078.045-.094.109-.048.192l.691 1.26A4.826 4.826 0 0 0 3.005 6h10a4.822 4.822 0 0 0-2.792-4.529zM4.505 3a.5.5 0 1 1 0-1 .5.5 0 0 1 0 1zm7 0a.5.5 0 1 1 0-1 .5.5 0 0 1 0 1z"/>
                    <path d="M3.005 7a4.414 4.414 0 0 0-1 2.897V14a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V9.897A4.414 4.414 0 0 0 13.005 7h-10z"/>
                </svg>
                <h5>GERAR APK</h5>
            </div>
        </div>`

    constructor() {
        super(document.createElement('div'), {
            height: '400px',
            width: '270px'
        });

        this.element.innerHTML = this.__html__;
    }

    setOnBtnCreateClick(callback) {
        this.element.querySelector('.__create').addEventListener('click', callback);
    }

    setOnBtnImportClick(callback) {
        this.element.querySelector('.__import').addEventListener('click', callback);
    }

    setOnBtnSyncClick(callback) {
        this.element.querySelector('.__sync').addEventListener('click', callback);
    }

    setOnBtnApkDownloadClick(callback) {
        this.element.querySelector('.__apk_download').addEventListener('click', callback);
    }
}

export default CardDefault;
