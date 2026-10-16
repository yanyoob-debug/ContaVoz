import test from 'node:test';
import assert from 'node:assert/strict';
import { todayISO, formatDate, formatMonth, shiftMonth } from '../../main/resources/static/dates.mjs';

test('data padrão respeita São Paulo mesmo perto da meia-noite UTC', () => {
  assert.equal(todayISO(new Date('2026-10-01T01:00:00Z')), '2026-09-30');
  assert.equal(todayISO(new Date('2026-10-01T03:00:00Z')), '2026-10-01');
});

test('mostra a data civil sem deslocamento de fuso e preserva ausência de data', () => {
  assert.equal(formatDate('2026-10-01'), '01/10/2026');
  assert.equal(formatDate(null), 'Sem data');
  assert.equal(formatMonth('2026-10'), 'outubro de 2026');
});

test('navega entre meses e anos respeitando os limites do seletor', () => {
  assert.equal(shiftMonth('2026-01', -1), '2025-12');
  assert.equal(shiftMonth('2026-12', 1), '2027-01');
  assert.equal(shiftMonth('2026-10', 1), '2026-11');
  assert.equal(shiftMonth('0001-01', -1), '0001-01');
  assert.equal(shiftMonth('9999-12', 1), '9999-12');
});
