"""Executa a coleção contra um banco local de avaliação e salva respostas reais."""
import argparse, base64, json, pathlib, uuid
from urllib import request, error, parse

parser = argparse.ArgumentParser()
parser.add_argument('--base-url', default='http://localhost:3000')
parser.add_argument('--saida', default='postman/projeto-integrador.postman_collection.json')
args = parser.parse_args()
base = args.base_url.rstrip('/')
if parse.urlparse(base).hostname not in ('localhost', '127.0.0.1'):
    raise SystemExit('O coletor só pode executar contra uma instalação local de avaliação.')
http = request.build_opener(request.ProxyHandler({}))
variables = {'baseUrl': base, 'adminUsuario': 'avaliador', 'adminSenha': 'Avaliacao-Admin123',
             'atendenteUsuario': 'operador', 'atendenteSenha': 'Avaliacao-Atendente123'}
items = []

def resolve(value):
    for key, val in variables.items():
        value = value.replace('{{' + key + '}}', str(val))
    return value

def run(name, method, path, expected=200, data=None, role='admin', capture=None, content_type='application/json', extra_headers=None, checks=None):
    prefix = 'admin' if role == 'admin' else 'atendente'
    auth = {'type': 'noauth'} if role is None else {'type': 'basic', 'basic': [
        {'key': 'username', 'value': '{{' + prefix + 'Usuario}}', 'type': 'string'},
        {'key': 'password', 'value': '{{' + prefix + 'Senha}}', 'type': 'string'}]}
    if role == 'invalid':
        auth = {'type': 'basic', 'basic': [{'key': 'username', 'value': 'inexistente', 'type': 'string'},
                                        {'key': 'password', 'value': 'incorreta', 'type': 'string'}]}
    req = {'method': method, 'auth': auth, 'header': [], 'url': '{{baseUrl}}' + path}
    headers = dict(extra_headers or {})
    req['header'].extend({'key': k, 'value': v} for k, v in headers.items())
    body = None
    if data is not None:
        raw = data if isinstance(data, str) else json.dumps(data, ensure_ascii=False)
        req['body'] = {'mode': 'raw', 'raw': raw, 'options': {'raw': {'language': 'json'}}}
        req['header'].append({'key': 'Content-Type', 'value': content_type})
        headers['Content-Type'] = content_type
        body = resolve(raw).encode()
    if role is not None:
        user, password = ('inexistente', 'incorreta') if role == 'invalid' else (variables[prefix + 'Usuario'], variables[prefix + 'Senha'])
        headers['Authorization'] = 'Basic ' + base64.b64encode((user + ':' + password).encode()).decode()
    native = request.Request(base + resolve(path), data=body, headers=headers, method=method)
    try:
        result = http.open(native, timeout=30)
    except error.HTTPError as exc:
        result = exc
    status = result.code
    text = result.read().decode()
    if status != expected:
        raise SystemExit(f'{name}: esperado {expected}; recebido {status}: {text}')
    payload = json.loads(text) if text else None
    events = ['pm.test("HTTP ' + str(expected) + '", function () { pm.response.to.have.status(' + str(expected) + '); });']
    if expected >= 400:
        if not isinstance(payload, dict) or not isinstance(payload.get('message'), str) or not payload['message'].strip():
            raise SystemExit(name + ': resposta de erro sem mensagem compreensível')
        events += ['pm.test("Mensagem de erro preenchida", function () { pm.expect(pm.response.json().message).to.be.a("string").and.not.empty; });']
    if capture or checks:
        events += ['const dados = pm.response.json();']
    if capture:
        for variable, key in capture.items():
            variables[variable] = payload[key]
        events += ['pm.collectionVariables.set(' + json.dumps(variable) + ', dados[' + json.dumps(key) + ']);' for variable, key in capture.items()]
    for field, value in (checks or {}).items():
        actual = payload
        expression = 'dados'
        for key in field.split('.'):
            actual = actual[key]
            expression += '[' + json.dumps(key) + ']'
        if actual != value:
            raise SystemExit(name + ': conteúdo inesperado em ' + field)
        events += ['pm.test(' + json.dumps('Conferir ' + field) + ', function () { pm.expect(' + expression + ').to.eql(' + json.dumps(value, ensure_ascii=False) + '); });']
    item = {'name': name, 'request': req, 'event': [{'listen': 'test', 'script': {'type': 'text/javascript', 'exec': events}}],
            'response': [{'name': name + ' — ' + str(status), 'originalRequest': req, 'status': result.reason, 'code': status,
                          '_postman_previewlanguage': 'json', 'header': [{'key': k, 'value': v} for k, v in result.headers.items() if k.lower() not in ('set-cookie', 'authorization')],
                          'cookie': [], 'body': text}]}
    items.append(item)
    print(str(status) + ' ' + name, flush=True)
    return payload

state = json.loads(http.open(base + '/api/state', timeout=30).read())
if state['current'] or state['waiting'] or state['paused']:
    raise SystemExit('O banco de avaliação deve estar sem fila ativa e com emissão liberada.')
run('01 — Acesso protegido sem autenticação', 'GET', '/api/admin/servicos', 401, role=None)
run('02 — Credenciais inválidas', 'GET', '/api/admin/servicos', 401, role='invalid')
run('03 — Administrador autenticado', 'GET', '/api/session')
run('04 — Atendente autenticado', 'GET', '/api/session', role='atendente')
run('05 — Atendente sem autorização administrativa', 'GET', '/api/admin/servicos', 403, role='atendente')
suffix = uuid.uuid4().hex[:8]
run('06 — Criar categoria de serviço', 'POST', '/api/admin/categorias', 201, {'nome': 'Cortes bovinos ' + suffix, 'ativo': True}, capture={'categoriaId': 'id'})
run('07 — Consultar categoria individual', 'GET', '/api/admin/categorias/{{categoriaId}}')
run('08 — Listar categorias', 'GET', '/api/admin/categorias')
run('09 — Atualizar categoria', 'PUT', '/api/admin/categorias/{{categoriaId}}', data={'nome': 'Preparos especiais ' + suffix, 'ativo': True})
run('10 — Categoria com nome vazio', 'POST', '/api/admin/categorias', 400, {'nome': ' ', 'ativo': True})
run('11 — Criar serviço vinculado à categoria', 'POST', '/api/admin/servicos', 201, {'nome': 'Cubos para panela ' + suffix, 'ativo': True, 'categoriaId': '{{categoriaId}}'}, capture={'servicoId': 'id', 'servicoNome': 'nome'})
run('12 — Consultar serviço individual', 'GET', '/api/admin/servicos/{{servicoId}}')
run('13 — Atualizar serviço', 'PUT', '/api/admin/servicos/{{servicoId}}', data={'nome': 'Carne em cubos ' + suffix, 'ativo': True, 'categoriaId': '{{categoriaId}}'}, capture={'servicoNome': 'nome'})
run('14 — Consulta JPA: serviços por categoria', 'GET', '/api/consultas/servicos-por-categoria/{{categoriaId}}', role='atendente')
run('15 — Impedir exclusão de categoria vinculada', 'DELETE', '/api/admin/categorias/{{categoriaId}}', 409)
run('16 — Impedir nome de serviço duplicado', 'POST', '/api/admin/servicos', 409, {'nome': '{{servicoNome}}', 'ativo': True, 'categoriaId': '{{categoriaId}}'})
run('17 — Rejeitar categoria inexistente', 'POST', '/api/admin/servicos', 404, {'nome': 'Categoria inválida', 'ativo': True, 'categoriaId': 'inexistente'})
run('18 — Criar balcão', 'POST', '/api/admin/balcoes', 201, {'nome': 'Balcão de apoio ' + suffix, 'ativo': False}, capture={'balcaoId': 'id'})
run('19 — Consultar balcão individual', 'GET', '/api/admin/balcoes/{{balcaoId}}')
run('20 — Listar balcões', 'GET', '/api/admin/balcoes')
run('21 — Atualizar balcão', 'PUT', '/api/admin/balcoes/{{balcaoId}}', data={'nome': 'Balcão de retirada ' + suffix, 'ativo': False})
run('22 — Excluir balcão sem vínculo', 'DELETE', '/api/admin/balcoes/{{balcaoId}}', 204)
run('23 — Confirmar exclusão do balcão', 'GET', '/api/admin/balcoes/{{balcaoId}}', 404)
run('24 — Criar usuário de teste', 'POST', '/api/admin/usuarios', 201, {'nome': 'auxiliar-' + suffix, 'email': 'auxiliar-' + suffix + '@example.com', 'senha': 'Auxiliar-Test123', 'perfil': 'ATENDENTE', 'ativo': True}, capture={'usuarioId': 'id'})
run('25 — Consultar usuário individual', 'GET', '/api/admin/usuarios/{{usuarioId}}')
run('26 — Listar usuários', 'GET', '/api/admin/usuarios')
run('27 — Atualizar usuário', 'PUT', '/api/admin/usuarios/{{usuarioId}}', data={'nome': 'apoio-' + suffix, 'email': 'apoio-' + suffix + '@example.com', 'perfil': 'ATENDENTE', 'ativo': False})
run('28 — Rejeitar e-mail inválido', 'POST', '/api/admin/usuarios', 400, {'nome': 'invalid', 'email': 'email-invalido', 'senha': 'Auxiliar-Test123', 'perfil': 'ATENDENTE', 'ativo': True})
run('29 — Excluir usuário sem histórico', 'DELETE', '/api/admin/usuarios/{{usuarioId}}', 204)
run('30 — Confirmar exclusão do usuário', 'GET', '/api/admin/usuarios/{{usuarioId}}', 404)
run('31 — Emitir senha pública', 'POST', '/api/tickets', 201, {'service': '{{servicoNome}}', 'origin': 'app'}, role=None, capture={'senhaToken': 'token'})
run('32 — Acompanhar a própria senha', 'GET', '/api/tickets/{{senhaToken}}', role=None, capture={'senhaId': 'id'})
run('33 — Consultar senha por ID', 'GET', '/api/gestao/senhas/{{senhaId}}')
run('34 — Listar senhas', 'GET', '/api/gestao/senhas')
run('35 — Editar serviço de senha aguardando', 'PUT', '/api/gestao/senhas/{{senhaId}}', data={'service': 'Moído'})
run('36 — Excluir senha aguardando', 'DELETE', '/api/gestao/senhas/{{senhaId}}', 204)
run('37 — Confirmar exclusão da senha', 'GET', '/api/gestao/senhas/{{senhaId}}', 404)
run('38 — Emitir senha para atendimento', 'POST', '/api/tickets', 201, {'service': 'Moído', 'origin': 'totem'}, role=None, capture={'senhaToken': 'token'})
run('39 — Consultar senha emitida', 'GET', '/api/tickets/{{senhaToken}}', role=None, capture={'senhaId': 'id'})
run('40 — Consulta JPA: senhas aguardando', 'GET', '/api/consultas/senhas-por-situacao?status=waiting', role='atendente')
run('41 — Pausar emissão', 'POST', '/api/queue/pause', role='atendente')
run('42 — Bloquear emissão enquanto pausada', 'POST', '/api/tickets', 409, {'service': 'Moído', 'origin': 'app'}, role=None)
run('43 — Retomar emissão', 'POST', '/api/queue/pause', role='atendente')
run('44 — Não iniciar sem uma chamada', 'POST', '/api/queue/start', 409, role='atendente')
run('45 — Chamar próxima senha', 'POST', '/api/queue/call', role='atendente')
run('46 — Não chamar dois clientes simultaneamente', 'POST', '/api/queue/call', 409, role='atendente')
run('47 — Preservar senha chamada', 'DELETE', '/api/gestao/senhas/{{senhaId}}', 409)
run('48 — Repetir chamada', 'POST', '/api/queue/recall', role='atendente')
run('49 — Iniciar atendimento após chegada', 'POST', '/api/queue/start', role='atendente')
run('50 — Concluir atendimento', 'POST', '/api/queue/finish', role='atendente')
run('51 — Consulta JPA: senhas concluídas', 'GET', '/api/consultas/senhas-por-situacao?status=done', role='atendente')
run('52 — Preservar histórico concluído', 'DELETE', '/api/gestao/senhas/{{senhaId}}', 409)
run('53 — Consultar indicadores reais', 'GET', '/api/state', role=None)
run('54 — Consultar auditoria com responsável', 'GET', '/api/audit', role='atendente')
run('55 — Rejeitar situação inválida', 'GET', '/api/consultas/senhas-por-situacao?status=invalido', 400, role='atendente')
run('56 — Recurso inexistente', 'GET', '/api/admin/servicos/inexistente', 404)
run('57 — Excluir serviço sem vínculo', 'DELETE', '/api/admin/servicos/{{servicoId}}', 204)
run('58 — Excluir categoria sem vínculo', 'DELETE', '/api/admin/categorias/{{categoriaId}}', 204)
run('59 — Listar serviços com administrador', 'GET', '/api/admin/servicos')
run('60 — Rejeitar consulta sem parâmetro obrigatório', 'GET', '/api/consultas/senhas-por-situacao', 400, role='atendente', checks={'errors.status': "Informe o parâmetro obrigatório 'status'."})
run('61 — Rejeitar Content-Type incompatível', 'POST', '/api/admin/categorias', 415, {'nome': 'Formato incorreto', 'ativo': True}, content_type='text/plain', checks={'message': 'Formato de conteúdo não suportado. Envie JSON com Content-Type: application/json.'})
run('62 — Identificar cada campo inválido', 'POST', '/api/admin/usuarios', 400, {'nome': ' ', 'email': 'email-invalido', 'senha': 'x' * 73, 'perfil': 'OUTRO', 'ativo': True}, checks={'errors.nome': 'Informe o nome.', 'errors.email': 'Informe um e-mail válido.', 'errors.senha': 'A senha deve ter até 72 caracteres.', 'errors.perfil': 'O perfil deve ser ADMIN ou ATENDENTE.'})
run('63 — Rejeitar JSON malformado', 'POST', '/api/admin/categorias', 400, '{', checks={'message': 'Corpo ausente ou JSON inválido. Envie um objeto JSON válido.'})
run('64 — Identificar parâmetro com tipo inválido', 'GET', '/api/admin/servicos?page=texto', 400, checks={'errors.page': "O parâmetro 'page' possui um valor inválido."})
run('65 — Rejeitar formato de resposta não disponível', 'GET', '/api/admin/servicos', 406, extra_headers={'Accept': 'application/xml'}, checks={'message': 'Formato de resposta não disponível. Utilize Accept: application/json.'})
collection = {'info': {'name': 'Corte Ágil — Projeto Integrador', 'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
                      'description': 'Requisições e respostas reais de uma execução local com MySQL. Contas exclusivamente didáticas do perfil avaliacao. Execute na ordem. Os scripts atualizam IDs, verificam o código HTTP e conferem mensagens e campos inválidos. Os exemplos em response correspondem ao recurso Save Response do Postman.'},
              'variable': [{'key': k, 'value': str(v), 'type': 'string'} for k, v in variables.items()], 'item': items}
out = pathlib.Path(args.saida)
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(collection, ensure_ascii=False, indent=2) + '\n')
print(f'Coleção salva: {len(items)} requisições com respostas reais em {out}')
