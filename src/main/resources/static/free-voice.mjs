import { parseAmountToCents } from './money.mjs?v=currency-mask-1';

const normalize = value => value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
const numbers = { zero: 0, um: 1, uma: 1, dois: 2, duas: 2, tres: 3, quatro: 4, cinco: 5, seis: 6, sete: 7, oito: 8, nove: 9, dez: 10, onze: 11, doze: 12, treze: 13, quatorze: 14, catorze: 14, quinze: 15, dezesseis: 16, dezessete: 17, dezoito: 18, dezenove: 19, vinte: 20, trinta: 30, quarenta: 40, cinquenta: 50, sessenta: 60, setenta: 70, oitenta: 80, noventa: 90, cem: 100, cento: 100, duzentos: 200, trezentos: 300, quatrocentos: 400, quinhentos: 500, seiscentos: 600, setecentos: 700, oitocentos: 800, novecentos: 900 };
function spokenInteger(words) {
  const parts = words.trim().split(/\s+/).filter(word => word !== 'e');
  if (!parts.length || parts.some(word => !(word in numbers))) return null;
  return parts.reduce((sum, word) => sum + numbers[word], 0);
}

export function draftExpense(text, today) {
  const normalized = normalize(text);
  let amount = null;
  const digits = normalized.match(/(?:r\$\s*)?(\d+(?:\.\d{3})*(?:[,.]\d{1,2})?)\s*reais?\b/);
  if (digits) {
    try { amount = parseAmountToCents(digits[1]); } catch { /* Review form keeps the amount blank. */ }
  } else {
    const beforeReais = normalized.split(/\breais?\b/);
    if (beforeReais.length > 1) {
      const words = beforeReais[0].trim().split(/\s+/);
      const numberWords = [];
      while (words.length && (words.at(-1) in numbers || words.at(-1) === 'e')) numberWords.unshift(words.pop());
      const value = spokenInteger(numberWords.join(' '));
      if (value !== null && value > 0) amount = value * 100;
    }
  }
  // Cents spoken separately or other ambiguous amounts must be entered during review.
  if (/\bcentavos?\b|\bvirgula\b|\bmenos\b|-\s*\d/.test(normalized)) amount = null;
  const rules = [
    ['UNEXPECTED', /\b(imprevisto|inesperado|emergencia|urgente)\b/],
    ['GROCERIES', /\b(alimentacao|mercado|supermercado|restaurante|comida|almoco|jantar|padaria|delivery|ifood)\b/],
    ['PHARMA', /\b(saude|farmacia|remedio|medicamento|consulta|exame|hospital)\b/],
    ['AUTO', /\b(locomocao|transporte|uber|onibus|gasolina|combustivel|taxi|estacionamento)\b/],
    ['LEISURE', /\b(lazer|cinema|jogo|hobby|hobbies|festa|passeio|bar|saida)\b/],
  ];
  let date = today;
  if (/\bontem\b/.test(normalized)) {
    const value = new Date(`${today}T12:00:00Z`); value.setUTCDate(value.getUTCDate() - 1); date = value.toISOString().slice(0, 10);
  }
  return { description: text.trim().slice(0, 255), amount, category: rules.find(([, pattern]) => pattern.test(normalized))?.[0] || '', date };
}

export function setupFreeVoice({ button, transcript, review, status, onReview }) {
  const Recognition = globalThis.SpeechRecognition || globalThis.webkitSpeechRecognition;
  let recognition = null, listening = false;
  const update = () => { review.disabled = listening || !transcript.value.trim(); };
  transcript.addEventListener('input', update);
  review.addEventListener('click', () => { if (transcript.value.trim()) onReview(transcript.value); });
  if (!Recognition) {
    button.disabled = true;
    status.textContent = 'Ditado indisponível neste navegador. Tente no Chrome ou Edge, ou digite o comando abaixo para revisar e salvar.';
  }
  button.addEventListener('click', () => {
    if (listening) { recognition.stop(); return; }
    if (!Recognition) return;
    recognition = new Recognition(); recognition.lang = 'pt-BR'; recognition.interimResults = false; recognition.continuous = false;
    listening = true; button.textContent = 'Parar ditado'; update();
    status.textContent = 'Ouvindo… Diga, por exemplo: gastei cinquenta reais no mercado hoje.';
    let received = false, failed = false;
    recognition.onresult = event => {
      received = true; transcript.value = event.results[0][0].transcript;
      status.textContent = 'Confira o texto e clique em Revisar gasto. Nada foi salvo ainda.';
    };
    recognition.onerror = event => {
      failed = true;
      status.textContent = event.error === 'not-allowed' || event.error === 'service-not-allowed'
        ? 'Microfone ou ditado bloqueado. Permita o acesso nas configurações do navegador ou digite o comando.'
        : 'O ditado não ficou disponível. Confira a conexão, tente novamente ou digite o comando.';
    };
    recognition.onend = () => {
      listening = false; button.textContent = 'Ditar gasto sem OpenAI'; update();
      if (!received && !failed) status.textContent = 'Nenhuma fala identificada. Tente novamente ou digite o comando.';
    };
    try { recognition.start(); } catch {
      listening = false; button.textContent = 'Ditar gasto sem OpenAI'; update();
      status.textContent = 'Não foi possível iniciar o ditado. Digite o comando abaixo para continuar.';
    }
  });
  globalThis.addEventListener('pagehide', () => recognition?.abort());
  update();
}
