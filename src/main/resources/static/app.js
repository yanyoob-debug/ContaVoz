import { draftExpense, setupFreeVoice } from './free-voice.mjs?v=free-voice-1';
import { setupRecorder } from './recorder.mjs?v=microphone-1';
import { parseAmountToCents, formatMoney, formatCurrencyInput, formatCurrencyPaste } from './money.mjs?v=currency-mask-1';
import { todayISO, formatDate, formatMonth, shiftMonth } from './dates.mjs';

const $ = (id) => document.getElementById(id);
const categories = {
  GROCERIES: { name: 'Alimentação', color: '#f58220', pale: '#fff1e3', icon: 'i-bag', ink: '#a74700' },
  PHARMA: { name: 'Saúde', color: '#24996b', pale: '#e9f6ef', icon: 'i-cross', ink: '#176447' },
  AUTO: { name: 'Locomoção', color: '#347dd1', pale: '#eaf2fd', icon: 'i-car', ink: '#245b9c' },
  LEISURE: { name: 'Lazer', color: '#8b5ac2', pale: '#f3ecfb', icon: 'i-gamepad', ink: '#694092' },
  UNEXPECTED: { name: 'Imprevistos', color: '#df3947', pale: '#ffedf0', icon: 'i-alert', ink: '#a51f2b' },
};
const state = { page: 0, size: 5, category: '', totalPages: 0, generation: 0, saving: false, sending: false, aiEnabled: false, audioUrl: null };
let selectedAudio = null;
const recording = setupRecorder({
  button: $('record-audio'), label: $('record-label'), hint: $('record-hint'), preview: $('record-preview'),
  onBusy(busy) {
    $('audio-file').disabled = busy || !state.aiEnabled;
    $('audio-confirmation').hidden = busy || !selectedAudio;
    $('send-audio').disabled = busy || !selectedAudio || !state.aiEnabled;
  },
  onFile(file) { selectedAudio = file; $('audio-confirmation').hidden = false; $('audio-file').value = ''; $('file-label').textContent = file.name; $('voice-status').textContent = 'Gravação pronta. Clique em Confirmar e enviar áudio para executar seu comando.'; $('voice-status').classList.remove('error'); },
});
let toastTimer;
let editingId = null;
let deletingTransaction = null;
let deleting = false;
state.month = todayISO().slice(0, 7);
state.allPeriods = false;
function syncPeriodInputs() {
  const [year, month] = state.month.split('-');
  $('period-month').value = month;
  $('period-year').value = Number(year);
}
syncPeriodInputs();

function renderPeriod() {
  const label = state.allPeriods ? 'Todos os períodos' : formatMonth(state.month);
  $('chart-period').textContent = label;
  $('chart-description').textContent = state.allPeriods ? 'Distribuição de todos os gastos registrados' : `Distribuição dos gastos em ${label}`;
  $('metric-period').textContent = label;
  for (const id of ['period-month', 'period-year', 'apply-period', 'current-month']) $(id).disabled = state.allPeriods;
  $('previous-month').disabled = state.allPeriods || state.month === '0001-01';
  $('next-month').disabled = state.allPeriods || state.month === '9999-12';
  $('period-feedback').textContent = state.allPeriods ? 'Todos os períodos' : label.charAt(0).toUpperCase() + label.slice(1);
}

function element(tag, className, text) {
  const result = document.createElement(tag);
  if (className) result.className = className;
  if (text !== undefined) result.textContent = text;
  return result;
}

function icon(name) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  svg.setAttribute('aria-hidden', 'true');
  use.setAttribute('href', `#${name}`);
  svg.append(use);
  return svg;
}

async function responseError(response) {
  try {
    const problem = await response.json();
    return new Error(problem.detail || `A operação não foi concluída (HTTP ${response.status}).`);
  } catch {
    return new Error(`A operação não foi concluída (HTTP ${response.status}).`);
  }
}

async function api(path, options = {}) {
  const response = await fetch(path, { ...options, signal: options.signal || AbortSignal.timeout(15000) });
  if (!response.ok) throw await responseError(response);
  if (response.status === 204) return null;
  return response.json();
}

function friendlyError(error) {
  if (error.name === 'TimeoutError' || error.name === 'AbortError') return 'A resposta demorou mais que o esperado. Confira a conexão e tente atualizar.';
  if (error instanceof TypeError) return 'Não foi possível acessar a API. Confira se a aplicação está em execução.';
  return error.message || 'Não foi possível concluir a operação.';
}

function notify(message) {
  clearTimeout(toastTimer);
  $('toast').textContent = message;
  $('toast').hidden = false;
  toastTimer = setTimeout(() => { $('toast').hidden = true; }, 4500);
}

function renderSummary(summaries) {
  const total = summaries.reduce((sum, item) => sum + item.totalInCents, 0);
  const count = summaries.reduce((sum, item) => sum + item.count, 0);
  const sorted = [...summaries].sort((a, b) => b.totalInCents - a.totalInCents);
  $('total-spent').textContent = formatMoney(total);
  $('total-count').textContent = count.toLocaleString('pt-BR');
  $('top-category').textContent = total > 0 ? categories[sorted[0].category].name : 'Sem gastos';
  $('top-category-detail').textContent = total > 0
    ? `${Math.round(sorted[0].totalInCents / total * 100)}% do total registrado` : 'Nenhuma transação registrada';
  $('donut-count').textContent = count.toLocaleString('pt-BR');
  $('donut-count-label').textContent = count === 1 ? 'lançamento' : 'lançamentos';
  $('category-donut').setAttribute('aria-label', `${count} ${count === 1 ? 'lançamento' : 'lançamentos'}, total de ${formatMoney(total)}. ` + summaries.map((item) => `${categories[item.category].name}: ${formatMoney(item.totalInCents)}`).join('; '));
  let cursor = 0;
  const stops = summaries.map((item) => {
    const start = cursor;
    cursor += total ? item.totalInCents / total * 100 : 0;
    return `${categories[item.category].color} ${start}% ${cursor}%`;
  });
  $('category-donut').style.background = total ? `conic-gradient(${stops.join(',')})` : '#f0f0f0';
  const legend = document.createDocumentFragment();
  for (const item of summaries) {
    const category = categories[item.category];
    const percent = total ? item.totalInCents / total * 100 : 0;
    const row = element('div', 'legend-row');
    const top = element('div', 'legend-top');
    const name = element('span', 'legend-name');
    const dot = element('span', 'legend-dot');
    dot.style.background = category.color;
    name.append(dot, document.createTextNode(category.name));
    top.append(name, element('strong', 'legend-amount', formatMoney(item.totalInCents)));
    const bottom = element('div', 'legend-bottom');
    const track = element('div', 'legend-track');
    const fill = element('div', 'legend-fill');
    fill.style.width = `${percent}%`;
    fill.style.background = category.color;
    track.append(fill);
    bottom.append(track, element('span', 'legend-percent', `${Math.round(percent)}%`));
    row.append(top, bottom);
    legend.append(row);
  }
  $('category-legend').replaceChildren(legend);
  $('metrics').setAttribute('aria-busy', 'false');
}

function tableMessage(message) {
  const row = element('tr');
  const cell = element('td', 'table-state', message);
  cell.colSpan = 5;
  row.append(cell);
  $('transaction-rows').replaceChildren(row);
}

function renderTransactions(page) {
  state.totalPages = page.totalPages;
  if (!page.items.length) tableMessage(state.category ? 'Nenhum gasto nesta categoria.' : 'Nenhuma transação registrada.');
  else {
    const rows = document.createDocumentFragment();
    for (const transaction of page.items) {
      const category = categories[transaction.category];
      const row = element('tr');
      const description = element('td');
      const detail = element('div', 'description-cell');
      const badgeIcon = element('span', 'transaction-icon');
      badgeIcon.style.background = category.pale;
      badgeIcon.style.color = category.ink;
      badgeIcon.append(icon(category.icon));
      const text = element('span');
      text.append(element('span', 'transaction-description', transaction.description), element('span', 'transaction-id', `ID ${transaction.id.slice(0, 8).toUpperCase()}`));
      detail.append(badgeIcon, text);
      description.append(detail);
      const categoryCell = element('td');
      const badge = element('span', 'category-badge', category.name);
      badge.style.background = category.pale;
      badge.style.color = category.ink;
      categoryCell.append(badge);
      row.append(description, element('td', 'date-cell', formatDate(transaction.date)), categoryCell, element('td', 'amount-cell', formatMoney(transaction.amount)));
      const actions = element('td', 'actions-cell');
      const edit = element('button', 'row-action', 'Editar');
      edit.type = 'button';
      edit.setAttribute('aria-label', `Editar ${transaction.description}`);
      edit.addEventListener('click', () => openTransaction(transaction));
      const remove = element('button', 'row-action delete-action', 'Excluir');
      remove.type = 'button';
      remove.setAttribute('aria-label', `Excluir ${transaction.description}`);
      remove.addEventListener('click', () => {
        deletingTransaction = transaction;
        $('delete-description').textContent = `${transaction.description} · ${formatMoney(transaction.amount)}`;
        $('delete-error').hidden = true;
        $('delete-dialog').showModal();
        $('cancel-delete').focus();
      });
      actions.append(edit, remove);
      row.append(actions);
      rows.append(row);
    }
    $('transaction-rows').replaceChildren(rows);
  }
  const first = page.totalElements ? page.page * page.size + 1 : 0;
  const last = Math.min((page.page + 1) * page.size, page.totalElements);
  $('page-info').textContent = `${first}–${last} de ${page.totalElements} ${page.totalElements === 1 ? 'transação' : 'transações'}`;
  $('page-number').textContent = page.page + 1;
  $('previous-page').disabled = page.page <= 0;
  $('next-page').disabled = page.page + 1 >= page.totalPages;
}

async function loadDashboard() {
  const generation = ++state.generation;
  $('refresh').disabled = true;
  $('previous-page').disabled = true;
  $('next-page').disabled = true;
  $('transaction-rows').setAttribute('aria-busy', 'true');
  const query = new URLSearchParams({ page: state.page, size: state.size });
  if (state.category) query.set('category', state.category);
  if (!state.allPeriods) query.set('month', state.month);
  const periodQuery = state.allPeriods ? '' : `?month=${encodeURIComponent(state.month)}`;
  try {
    const [page, summaries] = await Promise.all([
      api(`/transactions?${query}`),
      Promise.all(Object.keys(categories).map((category) => api(`/transactions/${category}/summary${periodQuery}`))),
    ]);
    if (generation !== state.generation) return;
    if (page.page > 0 && page.page >= page.totalPages) {
      state.page = Math.max(0, page.totalPages - 1);
      return loadDashboard();
    }
    renderTransactions(page);
    renderSummary(summaries);
    renderPeriod();
    $('load-error').hidden = true;
  } catch (error) {
    if (generation !== state.generation) return;
    $('load-error').textContent = friendlyError(error) + ' Use o botão de atualizar para tentar novamente.';
    $('load-error').hidden = false;
    tableMessage('Não foi possível carregar os dados.');
    $('page-info').textContent = 'Dados indisponíveis';
  } finally {
    if (generation === state.generation) {
      $('refresh').disabled = false;
      $('transaction-rows').setAttribute('aria-busy', 'false');
    }
  }
}

async function loadConfiguration() {
  $('audio-file').disabled = true;
  try {
    const config = await api('/api/config');
    state.aiEnabled = config.aiEnabled;
    $('demo-notice').hidden = !config.demo;
    $('voice-demo').hidden = config.demo || config.aiEnabled;
    if (!config.aiEnabled && !config.demo) $('voice-demo').textContent = 'O assistente de áudio está desativado nesta aplicação.';
    $('audio-file').disabled = !config.aiEnabled;
    recording.setEnabled(config.aiEnabled);
    $('upload-zone').classList.toggle('disabled', !config.aiEnabled);
  } catch {
    $('voice-demo').textContent = 'Não foi possível verificar a disponibilidade do assistente. Atualize a página para tentar novamente.';
    $('voice-demo').hidden = false;
  }
}

async function checkHealth() {
  try {
    const health = await api('/actuator/health');
    if (health.status !== 'UP') throw new Error('offline');
    $('connection-label').textContent = 'API conectada';
    $('connection-dot').className = 'status-dot online';
  } catch {
    $('connection-label').textContent = 'API indisponível';
    $('connection-dot').className = 'status-dot offline';
  }
}

function closeDialog() { if (!state.saving) $('transaction-dialog').close(); }
function openTransaction(transaction = null) {
  editingId = transaction?.id || null;
  $('transaction-form').reset();
  $('transaction-date').value = transaction ? (transaction.date || '') : todayISO();
  $('dialog-title').textContent = transaction ? 'Editar gasto' : 'Novo gasto';
  $('save-transaction').textContent = transaction ? 'Salvar alterações' : 'Salvar gasto';
  if (transaction) {
    $('description').value = transaction.description;
    $('amount').value = formatCurrencyInput(String(transaction.amount));
    $('category').value = transaction.category;
  }
  $('form-error').hidden = true;
  $('transaction-dialog').showModal();
  $('description').focus();
}
$('new-transaction').addEventListener('click', () => openTransaction());
setupFreeVoice({
  button: $('free-dictate'), transcript: $('free-transcript'), review: $('free-review'), status: $('free-status'),
  onReview(text) {
    const draft = draftExpense(text, todayISO());
    openTransaction();
    $('dialog-title').textContent = 'Confirmar gasto ditado';
    $('description').value = draft.description;
    $('amount').value = draft.amount === null ? '' : formatCurrencyInput(String(draft.amount));
    $('category').value = draft.category;
    $('transaction-date').value = draft.date;
    $('free-status').textContent = 'Revise descrição, valor, categoria e data. O gasto só será cadastrado ao clicar em Salvar gasto.';
  },
});
$('amount').addEventListener('input', () => {
  const input = $('amount');
  const rightDigits = input.value.slice(input.selectionStart ?? input.value.length).replace(/\D/g, '').length;
  input.value = formatCurrencyInput(input.value);
  let position = input.value.length;
  let remaining = rightDigits;
  while (position > 0 && remaining > 0) {
    position--;
    if (/\d/.test(input.value[position])) remaining--;
  }
  input.setSelectionRange(position, position);
});
$('amount').addEventListener('paste', (event) => {
  event.preventDefault();
  try {
    $('amount').value = formatCurrencyPaste(event.clipboardData.getData('text'));
    $('amount').setSelectionRange($('amount').value.length, $('amount').value.length);
    $('form-error').hidden = true;
  } catch {
    $('form-error').textContent = 'Cole apenas um valor positivo, como 481,22 ou R$ 1.250,00.';
    $('form-error').hidden = false;
  }
});
$('close-dialog').addEventListener('click', closeDialog);
$('cancel-dialog').addEventListener('click', closeDialog);
$('transaction-dialog').addEventListener('cancel', (event) => { if (state.saving) event.preventDefault(); });
$('transaction-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (state.saving) return;
  $('form-error').hidden = true;
  try {
    const description = $('description').value.trim();
    if (!description) throw new Error('Escreva uma descrição para o gasto.');
    const amount = parseAmountToCents($('amount').value);
    const date = $('transaction-date').value;
    if (!date) throw new Error('Informe a data do gasto.');
    state.saving = true;
    $('save-transaction').disabled = true;
    $('save-transaction').textContent = 'Salvando…';
    const isEditing = Boolean(editingId);
    await api(isEditing ? `/transactions/${editingId}` : '/transactions', { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ description, amount, category: $('category').value, date }) });
    state.saving = false;
    $('transaction-dialog').close();
    state.page = 0;
    if (!state.allPeriods) {
      state.month = date.slice(0, 7);
      syncPeriodInputs();
    }
    if (state.category && state.category !== $('category').value) {
      state.category = '';
      $('category-filter').value = '';
    }
    notify(isEditing ? 'Gasto atualizado com sucesso.' : 'Gasto registrado com sucesso.');
    await loadDashboard();
  } catch (error) {
    $('form-error').textContent = friendlyError(error);
    $('form-error').hidden = false;
  } finally {
    state.saving = false;
    $('save-transaction').disabled = false;
    $('save-transaction').textContent = editingId ? 'Salvar alterações' : 'Salvar gasto';
  }
});

$('cancel-delete').addEventListener('click', () => { if (!deleting) $('delete-dialog').close(); });
$('delete-dialog').addEventListener('cancel', (event) => { if (deleting) event.preventDefault(); });
$('confirm-delete').addEventListener('click', async () => {
  if (deleting || !deletingTransaction) return;
  deleting = true;
  $('confirm-delete').disabled = true;
  $('cancel-delete').disabled = true;
  $('confirm-delete').textContent = 'Excluindo…';
  $('delete-error').hidden = true;
  try {
    await api(`/transactions/${deletingTransaction.id}`, { method: 'DELETE' });
    $('delete-dialog').close();
    deletingTransaction = null;
    notify('Gasto excluído com sucesso.');
    await loadDashboard();
  } catch (error) {
    $('delete-error').textContent = friendlyError(error);
    $('delete-error').hidden = false;
  } finally {
    deleting = false;
    $('confirm-delete').disabled = false;
    $('cancel-delete').disabled = false;
    $('confirm-delete').textContent = 'Excluir gasto';
  }
});

$('category-filter').addEventListener('change', () => { state.category = $('category-filter').value; state.page = 0; loadDashboard(); });
function selectPeriod(month) {
  state.month = month;
  state.page = 0;
  syncPeriodInputs();
  renderPeriod();
  loadDashboard();
}
$('period-form').addEventListener('submit', (event) => {
  event.preventDefault();
  if (state.allPeriods || !$('period-form').reportValidity()) return;
  const year = Number($('period-year').value);
  if (!Number.isInteger(year) || year < 1 || year > 9999) return;
  selectPeriod(`${String(year).padStart(4, '0')}-${$('period-month').value}`);
});
$('previous-month').addEventListener('click', () => selectPeriod(shiftMonth(state.month, -1)));
$('next-month').addEventListener('click', () => selectPeriod(shiftMonth(state.month, 1)));
$('current-month').addEventListener('click', () => selectPeriod(todayISO().slice(0, 7)));
$('period-mode').addEventListener('change', () => {
  state.allPeriods = $('period-mode').value === 'all';
  state.page = 0;
  syncPeriodInputs();
  renderPeriod();
  loadDashboard();
});
$('refresh').addEventListener('click', () => { loadDashboard(); checkHealth(); });
$('previous-page').addEventListener('click', () => { if (state.page > 0) { state.page--; loadDashboard(); } });
$('next-page').addEventListener('click', () => { if (state.page + 1 < state.totalPages) { state.page++; loadDashboard(); } });

$('audio-file').addEventListener('change', () => {
  const file = $('audio-file').files[0];
  recording.clearPreview();
  selectedAudio = null;
  $('audio-confirmation').hidden = true;
  $('voice-status').textContent = '';
  $('voice-status').classList.remove('error');
  $('send-audio').disabled = true;
  $('file-label').textContent = file ? file.name : 'Escolher arquivo de áudio';
  if (!file) return;
  if (!file.size || file.size > 10 * 1024 * 1024 || !(file.type.startsWith('audio/') || /\.(m4a|mp3|wav|webm|mp4|mpeg|mpga|ogg|flac)$/i.test(file.name))) {
    $('voice-status').textContent = 'Escolha um áudio não vazio de até 10 MB.';
    $('voice-status').classList.add('error');
    return;
  }
  selectedAudio = file;
  $('audio-confirmation').hidden = false;
  $('send-audio').disabled = !state.aiEnabled;
});
$('voice-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!state.aiEnabled || state.sending || !selectedAudio || $('send-audio').disabled) return;
  state.sending = true;
  $('send-audio-label').textContent = 'Processando áudio…';
  recording.setEnabled(false);
  $('send-audio').disabled = true;
  $('audio-file').disabled = true;
  $('voice-status').classList.remove('error');
  $('voice-status').textContent = 'Ouvindo, interpretando e preparando sua resposta…';
  $('voice-response').pause();
  $('voice-response').hidden = true;
  if (state.audioUrl) { URL.revokeObjectURL(state.audioUrl); state.audioUrl = null; }
  const body = new FormData();
  body.append('file', selectedAudio);
  try {
    const response = await fetch('/transactions/ai', { method: 'POST', body, signal: AbortSignal.timeout(120000) });
    if (!response.ok) throw await responseError(response);
    state.audioUrl = URL.createObjectURL(await response.blob());
    $('voice-response').src = state.audioUrl;
    $('voice-response').hidden = false;
    $('voice-status').textContent = 'Comando processado. Ouça a resposta e confira suas transações.';
    $('audio-file').value = '';
    $('file-label').textContent = 'Escolher outro arquivo de áudio';
    state.page = 0;
  } catch (error) {
    $('voice-status').textContent = friendlyError(error);
    $('voice-status').classList.add('error');
  } finally {
    state.sending = false;
    selectedAudio = null;
    $('audio-confirmation').hidden = true;
    $('send-audio-label').textContent = 'Confirmar e enviar áudio';
    $('audio-file').value = '';
    $('file-label').textContent = 'Escolher arquivo de áudio';
    recording.setEnabled(state.aiEnabled);
    $('audio-file').disabled = !state.aiEnabled;
    // Reenviar exige selecionar o arquivo novamente, evitando repetição acidental.
    $('send-audio').disabled = true;
    await loadDashboard();
  }
});

for (const link of document.querySelectorAll('.nav-item')) {
  link.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach((item) => item.classList.remove('active'));
    link.classList.add('active');
    $('view-name').textContent = link.getAttribute('href') === '#voice' ? 'Assistente de voz' : link.getAttribute('href') === '#transactions' ? 'Transações' : 'Visão geral';
  });
}

renderPeriod();
loadConfiguration();
loadDashboard();
checkHealth();




