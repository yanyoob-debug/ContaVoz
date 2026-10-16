import { test } from 'node:test';
import assert from 'node:assert/strict';
import { draftExpense, setupFreeVoice } from '../../main/resources/static/free-voice.mjs';

test('prepara rascunho com reais, categoria e data sem salvar nem chamar IA', () => {
  assert.deepEqual(draftExpense('Gastei cinquenta reais no mercado hoje', '2026-10-16'), {
    description: 'Gastei cinquenta reais no mercado hoje', amount: 5000, category: 'GROCERIES', date: '2026-10-16',
  });
  assert.equal(draftExpense('Paguei 12,50 reais de uber ontem', '2026-01-01').date, '2025-12-31');
  assert.equal(draftExpense('Paguei 12,50 reais de uber', '2026-10-16').amount, 1250);
  assert.equal(draftExpense('Cento e vinte reais no cinema', '2026-10-16').category, 'LEISURE');
  assert.equal(draftExpense('Cento e vinte reais no cinema', '2026-10-16').amount, 12000);
  assert.equal(draftExpense('Emergência de cinquenta reais na farmácia', '2026-10-16').category, 'UNEXPECTED');
});

test('valores ausentes ou ambíguos e categorias desconhecidas ficam para revisão', () => {
  for (const text of ['Comprei uma coisa', 'Paguei menos 50 reais', 'Paguei 10 reais e 50 centavos', 'dez vírgula cinco reais']) {
    assert.equal(draftExpense(text, '2026-10-16').amount, null);
  }
  assert.equal(draftExpense('Paguei 10 reais em alguma coisa', '2026-10-16').category, '');
});

test('navegador sem reconhecimento permite digitar e revisar sem abrir microfone', () => {
  const element = () => Object.assign(new EventTarget(), { disabled: false, value: '', textContent: '' });
  globalThis.addEventListener = () => {};
  const button = element(), transcript = element(), review = element(), status = element();
  let result;
  setupFreeVoice({ button, transcript, review, status, onReview(text) { result = text; } });
  assert.equal(button.disabled, true); assert.equal(review.disabled, true);
  transcript.value = 'Gastei 10 reais no mercado'; transcript.dispatchEvent(new Event('input'));
  assert.equal(review.disabled, false); assert.equal(result, undefined);
  review.dispatchEvent(new Event('click')); assert.equal(result, transcript.value);
});
