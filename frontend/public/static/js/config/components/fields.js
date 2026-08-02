class SelectFiled {
    constructor(options) {
        this._element = document.createElement('select')
        this._element.classList.add('form-select')

        this._label = document.createElement('label')
        this._label.classList.add('form-label')

        options.forEach(o => this.setOption(o))
    }

    setOption(option) {
        const element = document.createElement('option')
        element.value = option.value
        element.innerHTML = option.label
        this._element.appendChild(element)
    }

    setSelected(value) {
        this._element.childNodes.forEach(e => {
            if (e.value == value) {
                e.selected = true
            }
        })
    }

    setOnChange(callback) {
        this._element.onchange = callback
    }

    getSelected() {
        for (let e of this._element.childNodes) {
            if (e.selected) {
                return { value: e.value, label: e.innerHTML }
            }
        }
    }

    render() {
        const div = document.createElement('div')
        div.classList.add('d-flex', 'flex-column', 'w-100')

        div.appendChild(this._label)
        div.appendChild(this._element)

        return div
    }
}

export class InputFiled {
    constructor(element = document.createElement('input')) {
        this._element = element
        this._element.classList.add('form-control')

        this._label = document.createElement('label')
        this._label.classList.add('form-label')
    }

    setValue(value) {
        this._element.value = value
    }

    getValue() {
        return this._element.value
    }

    render() {
        const div = document.createElement('div')
        div.classList.add('d-flex', 'flex-column', 'w-100')

        div.appendChild(this._label)
        div.appendChild(this._element)

        return div
    }

    validate() { }
}

class TextFiled extends InputFiled {
    constructor() {
        super()
        this._element.type = 'text'
    }
}

class NumberFiled extends InputFiled {
    constructor() {
        super()
        this._element.type = 'number'
    }

    getValue() {
        return parseInt(super.getValue())
    }
}

class TextAreaFiled extends InputFiled {
    constructor() {
        super(document.createElement('textarea'))
        this._element.rows = 4;
    }
}

export class ConfigType extends SelectFiled {
    constructor(types) {
        super(types.map(v => ({ label: v, value: v })))
        this._label.innerText = 'Modo de conexão'
    }
}

export class ConfigCategory extends SelectFiled {
    constructor(categories) {
        super(categories.map(c => ({ label: c.name, value: c.id })))
        this._label.innerText = 'Categoria'
    }
}

export class ConfigTlsVersion extends SelectFiled {
    constructor(versions) {
        super(versions.map(v => ({ label: v, value: v })))
        this._label.innerHTML = 'Versão do TLS'
    }
}

export class ConfigName extends TextFiled {
    constructor(name) {
        super()
        this._element.value = name
        this._element.required = true
        this._label.innerHTML = 'Nome'
        this._element.placeholder = 'Ex: Nome';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina um NOME para configuração')
        }
    }
}

export class ConfigDesc extends TextFiled {
    constructor(desc) {
        super()
        this._element.value = desc
        this._label.innerHTML = 'Descrição'
        this._element.placeholder = 'Ex: Descrição';
    }

    setName(name) {
        this._element.value = name
    }

    getName() {
        return this._element.value
    }
}

export class ConfigOrder extends NumberFiled {
    constructor(order) {
        super()
        this._element.value = order
        this._label.innerHTML = 'Ordem'
        this._element.placeholder = 'Ex: 1';
    }
}

export class ConfigSni extends TextFiled {
    constructor(sni, optional = false) {
        super()
        this._element.value = sni
        this._optional = optional;
        this._label.innerHTML = 'SNI'
        this._element.placeholder = 'Ex: google.com.br';
    }

    validate() {
        if (!this.getValue() && !this._optional) {
            throw new Error('Defina uma SNI para configuração');
        }
    }
}

export class ConfigPayload extends TextAreaFiled {
    constructor(payload) {
        super()
        this._element.value = payload
        this._label.innerText = 'PAYLOAD'
        this._element.placeholder = 'GET / HTTP/1.1[crlf]Host: google.com[crlf][crlf]';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina uma PAYLOAD para configuração')
        }
    }
}

export class ConfigOpenVPN extends TextAreaFiled {
    constructor(payload) {
        super()
        this._element.value = payload
        this._label.innerText = 'OPENVPN'
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina o certificado OpenVPN para configuração')
        }
    }
}

export class ConfigV2ray extends TextAreaFiled {
    constructor(payload) {
        super()
        this._element.value = payload
        this._label.innerText = 'V2RAY'
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Configuração v2ray invalida!')
        }
    }
}

export class ConfigProxy extends TextFiled {
    constructor(proxy) {
        super()
        this._element.value = proxy
        this._label.innerHTML = 'Proxy'
        this._element.placeholder = 'Ex: 127.0.0.1';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina um PROXY para configuração')
        }
    }
}

export class ConfigServer extends TextFiled {
    constructor(server) {
        super()
        this._element.value = server
        this._label.innerHTML = 'Servidor'
        this._element.placeholder = 'Ex: 127.0.0.1';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina uma SERVIDOR para configuração')
        }
    }
}

export class ConfigPort extends NumberFiled {
    constructor(port) {
        super()
        this._element.value = port
        this._label.innerHTML = 'Porta'
        this._element.placeholder = 'Ex: 80';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Defina uma PORTA para configuração')
        }
    }
}

export class ConfigDns1 extends TextFiled {
    constructor(dns) {
        super()
        this._element.value = dns
        this._label.innerHTML = 'DNS 1'
        this._element.placeholder = 'Ex: 8.8.8.8';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('DNS1 invalido!')
        }
    }
}

export class ConfigDns2 extends TextFiled {
    constructor(dns) {
        super()
        this._element.value = dns
        this._label.innerHTML = 'DNS 2'
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('DNS2 invalido!')
        }
    }
}

export class ConfigUsername extends TextFiled {
    constructor(username) {
        super()
        this._element.value = username
        this._label.innerHTML = 'Nome de usuário'
        this._element.placeholder = 'Ex: light';
    }
}

export class ConfigPassword extends TextFiled {
    constructor(password) {
        super()
        this._element.value = password
        this._label.innerHTML = 'Senha'
        this._element.placeholder = 'Ex: 1234';
    }
}

export class ConfigUuid extends TextFiled {
    constructor(uuid) {
        super()
        this._element.value = uuid
        this._label.innerHTML = 'UUID'
        this._element.placeholder = 'Ex: 00000000-0000-0000-0000-000000000000';
    }
}

export class ConfigUdpPort extends TextFiled {
    constructor(port) {
        super()
        this._element.value = port
        this._label.innerHTML = 'Portas UDP'
        this._element.placeholder = 'Ex: 8080,8081,8083';
    }

    validate() {
        if (!this.getValue()) {
            throw new Error('Porta UDP invalida!')
        }
    }
}

export class ConfigUrlCheckUser extends TextFiled {
    constructor(url) {
        super()
        this._element.value = url
        this._label.innerHTML = 'URL Check User'
        this._element.placeholder = 'Ex: https://url.checkuser.com';

        this._btnCheckUrl = document.createElement('button');
        this._btnCheckUrl.type = 'button';
        this._btnCheckUrl.className = 'input-group-text btn-clipboard'
        this._btnCheckUrl.innerHTML = 'Testar'
        this._btnCheckUrl.onclick = () => this._checkUrlOKClick();
    }

    async _checkUrlOK(url, timeoutMilliseconds) {
        const timeoutPromise = new Promise((_, reject) => {
            setTimeout(() => {
                reject(new Error('Request timed out'));
            }, timeoutMilliseconds);
        });

        const fetchPromise = fetch(url, { mode: 'no-cors' });

        try {
            await Promise.race([fetchPromise, timeoutPromise]);
            return true;
        } catch (error) {
            throw error;
        }
    }

    async _checkUrlOKClick() {
        try {
            const isUrlOK = await this._checkUrlOK(this._element.value, 3000)
            if (isUrlOK) showToastSuccess('Sua url esta OK!')
            else showToastError('URL offline ou invalida!')
        } catch (e) {
            showToastError(`Erro: ${e.message}`)
        }
    }

    render() {
        const div = document.createElement('div')
        div.classList.add('d-flex', 'flex-column', 'w-100')
        div.appendChild(this._label)

        const group = document.createElement('div');
        group.classList.add('input-group')
        group.appendChild(this._element)
        group.appendChild(this._btnCheckUrl)

        div.appendChild(group)
        return div
    }
}

export class ConfigIcon extends TextFiled {
    constructor(url) {
        super()
        this._element.value = url
        this._label.innerHTML = 'Ícone'
        this._element.placeholder = 'Ex: https://icon.example.com/icon.png';

        this._upload = document.createElement('button');
        this._upload.type = 'button';
        this._upload.className = 'input-group-text btn-clipboard'
        this._upload.innerHTML = '<i class="bi bi-upload"></i>'
        this._upload.onclick = () => this._startUpload();

        this._view = document.createElement('button');
        this._view.type = 'button';
        this._view.className = 'input-group-text btn-clipboard'
        this._view.innerHTML = '<i class="bi bi-eye"></i>'
        this._view.onclick = () => this._viewImage();
    }

    _startUpload() {
        const element = document.createElement('input');
        element.type = 'file';
        element.onchange = async e => {
            const link = await uploadImage(e.target)
            this._element.value = link;
        };
        element.click();
    }

    _viewImage() {
        if (!this.getValue()) {
            return;
        }

        const element = document.createElement('div');
        element.className = 'modal fade';
        element.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content bg-dark">
                <div class="modal-header">
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
            <div class="modal-body">
                <img class="img-fluid img-thumbnail" id="image" />
            </div>            
        </div>
        `
        element.querySelector('#image').src = this.getValue();
        const modal = new bootstrap.Modal(element);
        element.addEventListener('hide.bs.modal', () => setTimeout(() => element.remove(), 300));
        modal.show()
    }

    render() {
        const div = document.createElement('div')
        div.classList.add('d-flex', 'flex-column', 'w-100')
        div.appendChild(this._label)

        const group = document.createElement('div');
        group.classList.add('input-group')
        group.appendChild(this._element)
        group.appendChild(this._view)
        group.appendChild(this._upload)

        div.appendChild(group)
        return div
    }
}

export class ConfigDnsttKey extends TextAreaFiled {
  constructor(key) {
    super();
    this._element.value = key;
    this._label.innerText = 'DNSTT Public Key';
    this._element.placeholder = 'Ex: uRz...public-key';
  }

  validate() {
    if (!this.getValue()) {
      throw new Error('Defina uma KEY para configuração');
    }
  }
}

export class ConfigDnsttNameServer extends TextFiled {
  constructor(names) {
    super();
    this._element.value = names;
    this._label.innerText = 'DNSTT Name Server';
    this._element.placeholder = 'Ex: ns.example.com';
  }

  validate() {
    if (!this.getValue()) {
      throw new Error('Defina um NOME DO SERVIDOR para configuração');
    }
  }
}

export class ConfigDnsttServer extends TextFiled {
  constructor(dns) {
    super();
    this._element.value = dns;
    this._label.innerText = 'DNS Resolver';
    this._element.placeholder = 'Ex: 8.8.8.8 or 1.1.1.1';
  }

  validate() {
    if (!this.getValue()) {
      throw new Error('Defina um DNS DO SERVIDOR para configuração');
    }
  }
}

export class ConfigHyObfs extends TextFiled {
  constructor(obfs) {
    super();
    this._element.value = obfs;
    this._label.innerText = 'Ofuscação de senha';
    this._element.placeholder = 'Ex: obfs_password';
  }
}

export class ConfigHyUpMbps extends NumberFiled {
  constructor(mbps) {
    super();
    this._element.value = mbps;
    this._label.innerText = 'Upload (Mbps)';
    this._element.placeholder = 'Ex: 100';
  }
}

export class ConfigHyDownMbps extends NumberFiled {
  constructor(mbps) {
    super();
    this._element.value = mbps;
    this._label.innerText = 'Download (Mbps)';
    this._element.placeholder = 'Ex: 150';
  }
}

export class ConfigHyInsecure extends SelectFiled {
  constructor(insecure) {
    super([
      { label: 'Sim', value: 'true' },
      { label: 'Não', value: 'false' },
    ]);
    this._label.innerText = 'Conexões inseguras';
    this.setSelected(insecure ? 'true' : 'false');
  }

  getValue() {
    return this.getSelected().value === 'true';
  }
}

export class ConfigHyPort extends TextFiled {
  constructor(port) {
    super();
    this._element.value = port;
    this._label.innerText = 'Porta';
    this._element.placeholder = 'Ex: 8080,10000-65535';
    this._element.pattern = '^(\\d{1,5})(,\\s*\\d{1,5})*$';
  }

  validate() {
    if (!this.getValue()) {
      throw new Error('Defina uma PORTA para configuração');
    }

    const ports = this.getValue()
      .split(',')
      .map((p) => p.trim());

    for (const port of ports) {
      const range = port.split('-');
      if (range.length === 2) {
        const start = parseInt(range[0]);
        const end = parseInt(range[1]);
        if (
          isNaN(start) ||
          isNaN(end) ||
          start < 1 ||
          end > 65535 ||
          start > end
        ) {
          throw new Error(`Intervalo de porta inválido: ${port}`);
        }
        return;
      }

      const portNumber = parseInt(port);
      if (isNaN(portNumber) || portNumber < 1 || portNumber > 65535) {
        throw new Error(`Porta inválida: ${port}`);
      }
    }
  }
}

export class ConfigHyVersion extends SelectFiled {
  constructor(version) {
    super([
      { label: '1', value: '1' },
      { label: '2', value: '2' },
    ]);
    this._label.innerText = 'Versão do Hysteria';
    this.setSelected(version.toString());
  }

  getValue() {
    return parseInt(this.getSelected().value);
  }
}