// Shows the shared "poem of the day" published by the infra/ CDK stack.
// TODAY_URL is the stack's TodayUrl output: update it (and the Android app's
// todayJsonUrl on branch `app`) if the stack moves to another bucket/region.
const TODAY_URL =
  'https://poemoftheday-outb8d7ccdd-gydlpcby3qdo.s3.us-east-1.amazonaws.com/today.json';

const ERROR_TEXT = 'No se pudo cargar el poema de hoy.';

function show(poem) {
  // textContent, never innerHTML: the poem is data, not markup.
  document.getElementById('author').textContent = poem.author || '';
  document.getElementById('title').textContent = poem.title;
  document.getElementById('text').textContent = poem.text;
  document.title = poem.title + ' · Un poema al día';
}

function showError() {
  document.getElementById('author').textContent = '';
  document.getElementById('title').textContent = '';
  document.getElementById('text').textContent = ERROR_TEXT;
}

// today.json is served with max-age=300; 'no-cache' revalidates so the page
// never shows yesterday's poem just after the daily run.
fetch(TODAY_URL, { cache: 'no-cache' })
  .then((response) => {
    if (!response.ok) throw new Error('HTTP ' + response.status);
    return response.json();
  })
  .then((poem) => {
    const valid =
      poem &&
      typeof poem.title === 'string' && poem.title.trim() &&
      typeof poem.text === 'string' && poem.text.trim();
    if (!valid) throw new Error('today.json has no title or text');
    show(poem);
  })
  .catch((error) => {
    console.error('Could not load today.json:', error);
    showError();
  });
