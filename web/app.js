const image = document.getElementById('shot-image');
const label = document.getElementById('shot-label');
const note = document.getElementById('shot-note');
const tabs = Array.from(document.querySelectorAll('.shot-tab'));

function selectShot(tab) {
  if (!tab || !image) return;
  const { src, label: nextLabel, note: nextNote } = tab.dataset;
  if (!src) return;
  tabs.forEach(item => {
    const active = item === tab;
    item.classList.toggle('is-active', active);
    item.setAttribute('aria-selected', String(active));
  });
  image.style.opacity = '0';
  const preload = new Image();
  preload.onload = () => {
    image.src = src;
    image.alt = nextLabel || 'Captura de Volt Trigger';
    image.style.opacity = '1';
  };
  preload.onerror = () => { image.style.opacity = '1'; };
  preload.src = src;
  if (label) label.textContent = nextLabel || '';
  if (note) note.textContent = nextNote || '';
}

tabs.forEach(tab => tab.addEventListener('click', () => selectShot(tab)));
