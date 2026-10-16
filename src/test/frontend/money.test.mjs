import test from 'node:test';
import assert from 'node:assert/strict';
import { parseAmountToCents, formatMoney, formatCurrencyInput, formatCurrencyPaste } from '../../main/resources/static/money.mjs';

test('converte reais para centavos sem aritmética de ponto flutuante', () => {
  for (const [input, cents] of [['12,50', 1250], ['12.5', 1250], ['0,29', 29], [' 42 ', 4200], ['001,01', 101], ['1.234,56', 123456]]) {
    assert.equal(parseAmountToCents(input), cents);
  }
});

test('rejeita entradas ambíguas, vazias, negativas e valores sem precisão segura', () => {
  for (const input of ['', '0', '0,00', '-10', '12.34,56', '12,345', '12e3', 'NaN', '90071992547410']) {
    assert.throws(() => parseAmountToCents(input));
  }
});

test('insere vírgula e milhares automaticamente sem perder centavos', () => {
  for (const [digits, display] of [['', ''], ['1', '0,01'], ['48', '0,48'], ['481', '4,81'], ['4812', '48,12'], ['48122', '481,22'], ['123456', '1.234,56']]) {
    assert.equal(formatCurrencyInput(digits), display);
    if (digits && Number(digits)) assert.equal(parseAmountToCents(display), Number(digits));
  }
  assert.equal(formatCurrencyInput('0,012'), '0,12');
  assert.equal(formatCurrencyInput('00048122'), '481,22');
});

test('colagem aceita reais formatados e rejeita negativos e formatos ambíguos', () => {
  assert.equal(formatCurrencyPaste('R$ 1.250,00'), '1.250,00');
  assert.equal(formatCurrencyPaste('481,22'), '481,22');
  assert.equal(formatCurrencyPaste('12,5'), '12,50');
  assert.equal(formatCurrencyPaste('48122'), '481,22');
  assert.throws(() => formatCurrencyPaste('-10,00'));
  assert.throws(() => formatCurrencyPaste('12.34,56'));
});

test('formata valores em reais com a localização brasileira', () => {
  assert.equal(formatMoney(1250).replace(/\s/g, ' '), 'R$ 12,50');
  assert.equal(formatMoney(0).replace(/\s/g, ' '), 'R$ 0,00');
});
