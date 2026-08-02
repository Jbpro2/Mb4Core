
class CdnModal {
    constructor(form) {
        this.form = form
        this.element = document.createElement('div')
        this.element.classList.add('modal-dialog', 'modal-dialog-centered')
        this.element.setAttribute('role', 'document')
        this.element.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">CDN</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                </div>
                <div class="modal-footer d-flex justify-content-between flex-nowrap">
                    <button type="button" class="btn-responsive w-100 me-2" data-bs-dismiss="modal">Fechar</button>
                    <button type="button" class="btn-responsive w-100">Salvar</button>
                </div>
            </div>
        `
    }

    setOnSave(fn) {
        const button = this.element.querySelector('.modal-footer button:last-child')
        button.addEventListener('click', fn)
    }

    show() {
        this._root = document.createElement('div')
        this._root.classList.add('modal', 'fade')
        this._root.setAttribute('tabindex', '-1')

        this.element.querySelector('.modal-body').appendChild(this.form.render())
        this._root.appendChild(this.element)

        this.modal = new bootstrap.Modal(this._root)
        this.modal.show()
    }

    hide() {
        if (this.modal) this.modal.hide()
    }
}

class CdnForm {
    __html = `
        <div class="mb-3 row">
            <div class="col-md-12 mb-2">
                <label class="form-label">Nome</label>
                <input type="text" class="__name form-control" required>
            </div>
            <div class="col-md-12">
                <label class="form-label">URL</label>
                <input type="text" class="__url form-control" required>
            </div>
        </div>
        <div class="mb-3 row">
            <div class="col-md-12">
                <label class="form-label">Status</label>
                <select class="__status form-select">
                    <option value="ACTIVE">ATIVO</option>
                    <option value="INACTIVE">INATIVO</option>
                </select>
            </div>
        </div>
        `

    constructor(cdn) {
        this.cdn = cdn

        this.element = document.createElement('form')
        this.element.classList.add('form-group')
        this.element.innerHTML = this.__html

        this.name = this.element.querySelector('.__name')
        this.url = this.element.querySelector('.__url')

        this.status = this.element.querySelector('.__status')
        this.setup()
    }

    setup() {
        this.name.addEventListener('input', e => {
            this.cdn.name = e.target.value
        })

        this.url.addEventListener('input', e => {
            this.cdn.url = e.target.value
        })

        this.status.addEventListener('input', e => {
            this.cdn.status = e.target.value
        })
    }

    validate() {
        if (this.element.checkValidity()) return true
        this.element.classList.add('was-validated')
    }

    render() {
        this.name.value = this.cdn.name
        this.url.value = this.cdn.url
        this.status.value = this.cdn.status
        return this.element
    }
}


export { CdnModal, CdnForm }