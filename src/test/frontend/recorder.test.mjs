import { test } from 'node:test';
import assert from 'node:assert/strict';
import { setupRecorder } from '../../main/resources/static/recorder.mjs';

test('grava localmente, libera o microfone e entrega arquivo só ao parar; orienta quando bloqueado', async () => {
  const element = () => Object.assign(new EventTarget(), {
    disabled: true, textContent: '', hidden: true, classList: { add() {}, remove() {} },
    setAttribute() {}, removeAttribute() {}, pause() {},
  });
  let stopped = 0, delivered;
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: {
    mediaDevices: { getUserMedia: async () => ({ getTracks: () => [{ stop() { stopped++; } }] }) },
  } });
  globalThis.addEventListener = () => {};
  globalThis.MediaRecorder = class extends EventTarget {
    static isTypeSupported() { return true; }
    mimeType = 'audio/webm'; state = 'inactive';
    start() { this.state = 'recording'; }
    stop() {
      this.state = 'inactive';
      const event = new Event('dataavailable'); event.data = new Blob(['voice'], { type: this.mimeType });
      this.dispatchEvent(event); this.dispatchEvent(new Event('stop'));
    }
  };
  const button = element(), label = element(), hint = element(), preview = element();
  const control = setupRecorder({ button, label, hint, preview, onBusy() {}, onFile(file) { delivered = file; } });
  control.setEnabled(true); assert.equal(button.disabled, false);
  button.dispatchEvent(new Event('click'));
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(label.textContent, 'Parar gravação');
  assert.equal(delivered, undefined);
  button.dispatchEvent(new Event('click'));
  assert.equal(delivered.name, 'gravacao.webm');
  assert.equal(delivered.size, 5); assert.equal(stopped, 1); assert.equal(preview.hidden, false);
  control.clearPreview(); assert.equal(preview.hidden, true);
  navigator.mediaDevices.getUserMedia = async () => { throw Object.assign(new Error(), { name: 'NotAllowedError' }); };
  button.dispatchEvent(new Event('click'));
  await new Promise(resolve => setImmediate(resolve));
  assert.match(hint.textContent, /Microfone bloqueado/); assert.equal(button.disabled, false);
  control.setEnabled(false); assert.equal(button.disabled, true);
});
