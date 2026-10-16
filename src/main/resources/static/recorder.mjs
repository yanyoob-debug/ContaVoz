export function setupRecorder({ button, label, hint, preview, onBusy, onFile }) {
  let recorder, stream, timer, previewUrl;
  let enabled = false;
  let busy = false;
  const supported = !!(navigator.mediaDevices?.getUserMedia && globalThis.MediaRecorder);
  const stopTracks = () => { stream?.getTracks().forEach(track => track.stop()); stream = null; };
  const clearPreview = () => {
    preview.pause(); preview.hidden = true; preview.removeAttribute('src');
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewUrl = null;
  };
  const reset = () => {
    clearTimeout(timer); stopTracks(); busy = false;
    label.textContent = 'Gravar pelo microfone';
    button.classList.remove('recording'); button.setAttribute('aria-pressed', 'false');
    button.disabled = !enabled; onBusy(false);
  };
  function setEnabled(value) { enabled = value && supported; button.disabled = !enabled || (busy && recorder?.state !== 'recording'); }
  if (!supported) hint.textContent = 'Este navegador não permite gravar aqui. Abra o endereço no Chrome ou Edge, ou escolha um arquivo de áudio.';
  button.setAttribute('aria-pressed', 'false');
  button.addEventListener('click', async () => {
    if (recorder?.state === 'recording') { button.disabled = true; recorder.stop(); return; }
    if (!enabled || busy) return;
    busy = true; button.disabled = true; onBusy(true);
    hint.textContent = 'Permita o acesso ao microfone na solicitação do navegador.';
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = ['audio/webm;codecs=opus', 'audio/mp4', 'audio/webm'].find(type => MediaRecorder.isTypeSupported(type));
      recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      const chunks = []; let size = 0; let failed = false;
      recorder.addEventListener('dataavailable', event => {
        if (event.data.size) { chunks.push(event.data); size += event.data.size; }
        if (size > 10 * 1024 * 1024 && recorder.state === 'recording') { failed = true; recorder.stop(); }
      });
      recorder.addEventListener('error', () => { failed = true; reset(); hint.textContent = 'Não foi possível gravar. Tente novamente ou escolha um arquivo.'; });
      recorder.addEventListener('stop', () => {
        const type = recorder.mimeType || 'audio/webm';
        reset();
        if (failed || !size || size > 10 * 1024 * 1024) { hint.textContent = 'Gravação vazia ou inválida. Grave novamente (até 10 MB).'; return; }
        clearPreview();
        const file = new File(chunks, type.includes('mp4') ? 'gravacao.m4a' : 'gravacao.webm', { type });
        previewUrl = URL.createObjectURL(file); preview.src = previewUrl; preview.hidden = false;
        onFile(file); onBusy(false);
        hint.textContent = 'Gravação pronta. Você pode ouvir antes de enviar ou gravar novamente.';
      });
      recorder.start(1000);
      button.disabled = false; label.textContent = 'Parar gravação';
      button.classList.add('recording'); button.setAttribute('aria-pressed', 'true');
      hint.textContent = 'Gravando… Fale seu comando e clique em Parar gravação (máximo de 60 segundos).';
      timer = setTimeout(() => { if (recorder.state === 'recording') recorder.stop(); }, 60000);
    } catch (error) {
      reset();
      hint.textContent = error.name === 'NotAllowedError'
        ? 'Microfone bloqueado. Permita o microfone nas configurações deste site no navegador e tente novamente.'
        : error.name === 'NotFoundError' ? 'Nenhum microfone encontrado. Conecte um microfone ou escolha um arquivo.'
        : 'Não foi possível acessar o microfone. Verifique se ele está disponível ou escolha um arquivo.';
    }
  });
  addEventListener('pagehide', () => { clearTimeout(timer); if (recorder?.state === 'recording') recorder.stop(); stopTracks(); clearPreview(); });
  return { setEnabled, clearPreview };
}
