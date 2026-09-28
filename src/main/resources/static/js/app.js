/* MessMate front end: plain JS, talks to the Spring Boot REST API under /api */
const $ = (s, r = document) => r.querySelector(s);
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const pad = n => String(n).padStart(2, '0');
const fmt = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const parse = s => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
const mondayOf = d => addDays(d, -((d.getDay() + 6) % 7));
const short = d => d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
const dayName = d => d.toLocaleDateString('en-IN', { weekday: 'short' });
const TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const LABEL = { BREAKFAST: 'Breakfast', LUNCH: 'Lunch', DINNER: 'Dinner' };
const CATS = ['MAIN', 'SIDE', 'DESSERT', 'BEVERAGE'];
const WORDS = ['Poor', 'Weak', 'Okay', 'Good', 'Great'];
const band = v => v >= 4 ? 'good' : v >= 3 ? 'mid' : 'low';

const S = {
  view: 'resident', tab: 'ratings', week: mondayOf(new Date()),
  residents: [], meals: [], avg: { perMeal: [], perDay: [] },
  rid: localStorage.getItem('mm-rid') || '', mine: [], skipped: new Set(),
  rating: 0, rateMeal: null, editFb: null,
  draft: null, editMeal: null, editRes: null, hcDate: fmt(new Date()), threshold: 3
};

/* ---------- api ---------- */
async function api(path, opts = {}) {
  const res = await fetch(path, {
    method: opts.method || 'GET',
    headers: { 'Content-Type': 'application/json' },
    body: opts.body ? JSON.stringify(opts.body) : undefined
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.message || `Request failed (${res.status})`);
  return data;
}
let toastTimer;
function toast(msg, bad = false) {
  const t = $('#toast');
  t.textContent = msg; t.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.className = '', 3200);
}

/* ---------- load + render ---------- */
async function refresh() {
  const ws = fmt(S.week);
  try {
    [S.residents, S.meals, S.avg] = await Promise.all([
      api('/api/residents'), api(`/api/meals?weekStart=${ws}`), api(`/api/reports/averages?weekStart=${ws}`)]);
    if (!S.residents.some(r => String(r.id) === S.rid)) S.rid = S.residents[0] ? String(S.residents[0].id) : '';
    if (S.rid) {
      const [mine, skipped] = await Promise.all([
        api(`/api/feedback?residentId=${S.rid}`), api(`/api/opt-outs?residentId=${S.rid}`)]);
      S.mine = mine; S.skipped = new Set(skipped);
    } else { S.mine = []; S.skipped = new Set(); }
  } catch (e) { toast(e.message, true); }
  render();
}

function render() {
  $('#weekLabel').textContent = `${short(S.week)} to ${short(addDays(S.week, 6))} ${addDays(S.week, 6).getFullYear()}`;
  document.querySelectorAll('.switch button').forEach(b => b.setAttribute('aria-selected', b.dataset.view === S.view));
  $('#v-resident').hidden = S.view !== 'resident';
  $('#v-committee').hidden = S.view !== 'committee';
  if (S.view === 'resident') renderResident(); else renderCommittee();
}

/* ---------- resident view ---------- */
function stamp(a) {
  return a ? `<span class="stamp ${band(a.average)}" title="${a.count} rating${a.count > 1 ? 's' : ''}"><b>${a.average.toFixed(1)}</b><small>${a.count}</small></span>`
           : `<span class="stamp none" title="No ratings yet">-</span>`;
}

function ticket(m, ds, t, todayS) {
  if (!m) return `<div class="ticket empty"><p>${LABEL[t]} not published yet</p></div>`;
  const a = S.avg.perMeal.find(x => x.mealId === m.id);
  const mine = S.mine.find(f => f.mealId === m.id);
  const skipped = S.skipped.has(m.id);
  const items = m.items.map(i => `<li><i class="mark ${i.veg ? '' : 'nonveg'}" title="${i.veg ? 'Veg' : 'Non-veg'}"></i>${esc(i.name)}</li>`).join('');
  let acts;
  if (skipped) acts = `<span class="note">You are skipping this</span><button class="ghost" data-act="unskip" data-meal="${m.id}">Undo</button>`;
  else if (mine) acts = `<span class="note">You gave ${mine.rating}/5</span><button class="ghost" data-act="rate" data-meal="${m.id}">Edit</button>`;
  else acts = `<button class="primary" data-act="rate" data-meal="${m.id}" ${ds > todayS ? 'disabled title="Ratings open on the meal day"' : ''}>Rate</button>`
            + `<button class="ghost" data-act="skip" data-meal="${m.id}" ${ds < todayS ? 'disabled title="This meal is over"' : ''}>Skip</button>`;
  return `<article class="ticket ${skipped ? 'skipped' : ''}"><header><h4>${LABEL[t]}</h4>${stamp(a)}</header><ul>${items}</ul><footer>${acts}</footer></article>`;
}

function renderResident() {
  const todayS = fmt(new Date());
  const opts = S.residents.map(r => `<option value="${r.id}" ${String(r.id) === S.rid ? 'selected' : ''}>${esc(r.name)} (${esc(r.roomNo)})</option>`).join('');
  const cols = [...Array(7)].map((_, i) => {
    const d = addDays(S.week, i), ds = fmt(d);
    const tickets = TYPES.map(t => ticket(S.meals.find(m => m.date === ds && m.mealType === t), ds, t, todayS)).join('');
    return `<section class="day ${ds === todayS ? 'today' : ''}"><h3><span>${dayName(d)}</span><b>${d.getDate()}</b></h3>${tickets}</section>`;
  }).join('');
  $('#v-resident').innerHTML = S.residents.length
    ? `<div class="who"><label for="who">Eating as</label><select id="who">${opts}</select></div><div class="days">${cols}</div>`
    : `<div class="panel"><h3>No residents yet</h3><p>Switch to Committee and add residents first.</p></div>`;
}

/* ---------- rating dialog ---------- */
function paintPicks() {
  $('#picks').innerHTML = [1, 2, 3, 4, 5].map(n =>
    `<button type="button" class="pick" role="radio" aria-checked="${S.rating === n}" data-act="pick" data-v="${n}"><b>${n}</b><small>${WORDS[n - 1]}</small></button>`).join('');
}
function openRate(mealId) {
  const m = S.meals.find(x => x.id === mealId);
  S.rateMeal = m; S.editFb = S.mine.find(f => f.mealId === mealId) || null;
  S.rating = S.editFb ? S.editFb.rating : 0;
  $('#rateTitle').textContent = `${LABEL[m.mealType]}, ${dayName(parse(m.date))} ${short(parse(m.date))}`;
  $('#rateItems').textContent = m.items.map(i => i.name).join(', ');
  $('#rateComment').value = S.editFb?.comment || '';
  $('#rateErr').textContent = '';
  $('#rateRemove').hidden = !S.editFb;
  paintPicks();
  $('#rateDlg').showModal();
}
async function saveRating() {
  const comment = $('#rateComment').value;
  try {
    if (S.editFb) await api(`/api/feedback/${S.editFb.id}`, { method: 'PUT', body: { rating: S.rating || null, comment } });
    else await api('/api/feedback', { method: 'POST', body: { residentId: +S.rid, mealId: S.rateMeal.id, rating: S.rating || null, comment } });
    $('#rateDlg').close(); toast('Rating saved'); refresh();
  } catch (e) { $('#rateErr').textContent = e.message; }
}

/* ---------- committee view ---------- */
async function renderCommittee() {
  document.querySelectorAll('.tabs button').forEach(b => b.setAttribute('aria-current', b.dataset.tab === S.tab));
  try {
    const g = await api('/api/reports/dashboard');
    $('#glance').innerHTML = [['Residents', g.residents], ['Meals published', g.meals], ['Menu items', g.menuItems],
      ['Ratings received', g.feedbackEntries], ['Overall average', g.overallAverage == null ? '-' : g.overallAverage.toFixed(2)]]
      .map(([l, v]) => `<div><b>${v}</b><span>${l}</span></div>`).join('');
    await ({ ratings: tabRatings, headcount: tabHeadcount, menu: tabMenu, residents: tabResidents, search: tabSearch }[S.tab])();
  } catch (e) { toast(e.message, true); }
}

async function tabRatings() {
  const ws = fmt(S.week);
  const low = await api(`/api/reports/low-rated?weekStart=${ws}&threshold=${S.threshold}`);
  const rows = [...Array(7)].map((_, i) => {
    const d = addDays(S.week, i), ds = fmt(d);
    const cells = TYPES.map(t => {
      const a = S.avg.perMeal.find(x => x.date === ds && x.mealType === t);
      return a ? `<td class="${band(a.average)} ${a.average < S.threshold ? 'flag' : ''}"><b>${a.average.toFixed(2)}</b><small>${a.count} rating${a.count > 1 ? 's' : ''}</small></td>` : `<td class="dim">-</td>`;
    }).join('');
    const day = S.avg.perDay.find(x => x.date === ds);
    return `<tr><th scope="row">${dayName(d)} ${d.getDate()}</th>${cells}${day ? `<td class="${band(day.average)}"><b>${day.average.toFixed(2)}</b><small>${day.count} in all</small></td>` : '<td class="dim">-</td>'}</tr>`;
  }).join('');
  const lowList = low.length ? low.map(m => `<li>${stamp(m)}<span><b>${LABEL[m.mealType]}</b>, ${dayName(parse(m.date))} ${short(parse(m.date))}<br><small class="dim">${describe(m.mealId)}</small></span></li>`).join('')
    : `<li class="dim">No meal is rated below ${S.threshold} this week.</li>`;
  $('#cbody').innerHTML = `
    <div class="panel"><h3>Average rating per meal and per day</h3>
      <div class="tblwrap"><table><thead><tr><th>Day</th><th>Breakfast</th><th>Lunch</th><th>Dinner</th><th>Day average</th></tr></thead><tbody>${rows}</tbody></table></div></div>
    <div class="panel"><h3>Low-rated meals</h3>
      <div class="thresh"><label for="th">Show meals rated below</label><input id="th" type="number" min="1" max="5" step="0.1" value="${S.threshold}"></div>
      <ul class="low-list">${lowList}</ul></div>`;
}
const describe = id => (S.meals.find(m => m.id === id)?.items || []).map(i => i.name).join(', ');

async function tabHeadcount() {
  const rows = await api(`/api/reports/headcount?date=${S.hcDate}`);
  const bars = rows.length ? rows.map(r => {
    const pct = r.totalResidents ? Math.round(r.expected / r.totalResidents * 100) : 0;
    return `<div class="plate"><h4>${LABEL[r.mealType]}</h4><div class="bar" title="${r.expected} eating, ${r.optedOut} skipping"><span class="in" style="width:${pct}%"></span><span class="out" style="width:${100 - pct}%"></span></div>
      <p><b>${r.expected}</b> plates <span class="dim">(${r.optedOut} skipping out of ${r.totalResidents} residents)</span></p></div>`;
  }).join('') : `<p class="dim">No meals are published for this date.</p>`;
  $('#cbody').innerHTML = `<div class="panel"><h3>Headcount</h3>
    <label class="field" style="max-width:14rem;margin-bottom:1rem">Date<input type="date" id="hcd" value="${S.hcDate}"></label>${bars}</div>`;
}

function blankDraft() {
  S.editMeal = null;
  S.draft = { date: fmt(S.week), mealType: 'BREAKFAST', items: [{ name: '', category: 'MAIN', veg: true }] };
}
function tabMenu() {
  if (!S.draft) blankDraft();
  const d = S.draft;
  const itemRows = d.items.map((it, i) => `<div class="itemrow">
      <input aria-label="Dish name" placeholder="Dish, e.g. Sambar" value="${esc(it.name)}" data-f="name" data-i="${i}" maxlength="80">
      <select aria-label="Category" data-f="category" data-i="${i}">${CATS.map(c => `<option ${c === it.category ? 'selected' : ''}>${c}</option>`).join('')}</select>
      <label><input type="checkbox" data-f="veg" data-i="${i}" ${it.veg ? 'checked' : ''}> Veg</label>
      <button type="button" class="ghost" data-act="rmItem" data-i="${i}" aria-label="Remove dish">Remove</button></div>`).join('');
  const list = S.meals.length ? S.meals.map(m => `<li><span><b>${dayName(parse(m.date))} ${short(parse(m.date))}, ${LABEL[m.mealType]}</b><br><small>${esc(describe(m.id))}</small></span>
      <span class="acts"><button class="ghost" data-act="editMeal" data-id="${m.id}">Edit</button><button class="danger" data-act="delMeal" data-id="${m.id}">Delete</button></span></li>`).join('')
    : `<li class="dim">Nothing published for this week yet.</li>`;
  $('#cbody').innerHTML = `<div class="grid2">
    <form class="panel" id="mealForm"><h3>${S.editMeal ? 'Update this meal' : 'Publish a meal'}</h3>
      <div class="row" style="margin-bottom:.8rem">
        <label class="field" style="flex:1">Date<input type="date" id="md" value="${d.date}"></label>
        <label class="field" style="flex:1">Meal<select id="mt">${TYPES.map(t => `<option value="${t}" ${t === d.mealType ? 'selected' : ''}>${LABEL[t]}</option>`).join('')}</select></label>
      </div>${itemRows}
      <div class="row" style="margin:.4rem 0"><button type="button" class="ghost" data-act="addItem">Add a dish</button></div>
      <p class="err" id="mealErr" role="alert"></p>
      <div class="row"><button class="primary" type="submit">${S.editMeal ? 'Save changes' : 'Publish meal'}</button>
        ${S.editMeal ? '<button type="button" class="ghost" data-act="cancelMeal">Cancel</button>' : ''}</div></form>
    <div class="panel"><h3>This week's menu</h3><ul class="list">${list}</ul></div></div>`;
}

function tabResidents() {
  const e = S.editRes;
  const list = S.residents.map(r => `<li><span><b>${esc(r.name)}</b><br><small>Room ${esc(r.roomNo)}, ${esc(r.email)}</small></span>
      <span class="acts"><button class="ghost" data-act="editRes" data-id="${r.id}">Edit</button><button class="danger" data-act="delRes" data-id="${r.id}">Delete</button></span></li>`).join('')
    || `<li class="dim">No residents yet.</li>`;
  $('#cbody').innerHTML = `<div class="grid2">
    <form class="panel" id="resForm"><h3>${e ? 'Update resident' : 'Add a resident'}</h3>
      <label class="field">Name<input id="rn" value="${esc(e?.name)}" maxlength="80"></label><br>
      <label class="field">Room number<input id="rr" value="${esc(e?.roomNo)}" maxlength="10"></label><br>
      <label class="field">Email<input id="re" value="${esc(e?.email)}" maxlength="120"></label>
      <p class="err" id="resErr" role="alert"></p>
      <div class="row"><button class="primary" type="submit">${e ? 'Save changes' : 'Add resident'}</button>${e ? '<button type="button" class="ghost" data-act="cancelRes">Cancel</button>' : ''}</div></form>
    <div class="panel"><h3>Residents (${S.residents.length})</h3><ul class="list">${list}</ul></div></div>`;
}

async function tabSearch() {
  $('#cbody').innerHTML = `<div class="panel"><h3>Find a dish</h3>
    <div class="row" style="margin-bottom:1rem"><label class="field" style="flex:2">Name<input id="sq" placeholder="biryani, dosa..."></label>
    <label class="field" style="flex:1">Category<select id="sc"><option value="">Any</option>${CATS.map(c => `<option>${c}</option>`).join('')}</select></label></div>
    <ul class="list" id="sres"></ul></div>`;
  runSearch();
}
async function runSearch() {
  try {
    const items = await api(`/api/menu-items?q=${encodeURIComponent($('#sq').value)}&category=${$('#sc').value}`);
    $('#sres').innerHTML = items.length ? items.map(i => `<li><span><i class="mark ${i.veg ? '' : 'nonveg'}"></i> <b>${esc(i.name)}</b> <small>${i.category}</small></span><small>${LABEL[i.mealType]}, ${short(parse(i.mealDate))}</small></li>`).join('')
      : `<li class="dim">No dish matches.</li>`;
  } catch (e) { toast(e.message, true); }
}

/* ---------- events ---------- */
document.addEventListener('click', async e => {
  const b = e.target.closest('[data-act]'); if (!b) return;
  const id = +b.dataset.id, meal = +b.dataset.meal;
  try {
    switch (b.dataset.act) {
      case 'view': S.view = b.dataset.view; return render();
      case 'prev': S.week = addDays(S.week, -7); return refresh();
      case 'next': S.week = addDays(S.week, 7); return refresh();
      case 'thisWeek': S.week = mondayOf(new Date()); return refresh();
      case 'tab': S.tab = b.dataset.tab; return render();
      case 'rate': return openRate(meal);
      case 'pick': S.rating = +b.dataset.v; return paintPicks();
      case 'rateCancel': return $('#rateDlg').close();
      case 'unrate': await api(`/api/feedback/${S.editFb.id}`, { method: 'DELETE' }); $('#rateDlg').close(); toast('Rating removed'); return refresh();
      case 'skip': await api('/api/opt-outs', { method: 'POST', body: { residentId: +S.rid, mealId: meal } }); toast('Meal skipped'); return refresh();
      case 'unskip': await api(`/api/opt-outs?residentId=${S.rid}&mealId=${meal}`, { method: 'DELETE' }); toast('Skip removed'); return refresh();
      case 'addItem': S.draft.items.push({ name: '', category: 'SIDE', veg: true }); return tabMenu();
      case 'rmItem': S.draft.items.splice(+b.dataset.i, 1); return tabMenu();
      case 'editMeal': { const m = S.meals.find(x => x.id === id); S.editMeal = m;
        S.draft = { date: m.date, mealType: m.mealType, items: m.items.map(i => ({ name: i.name, category: i.category, veg: i.veg })) }; return tabMenu(); }
      case 'cancelMeal': blankDraft(); return tabMenu();
      case 'delMeal': if (!confirm('Delete this meal along with its ratings?')) return;
        await api(`/api/meals/${id}`, { method: 'DELETE' }); toast('Meal deleted'); blankDraft(); return refresh();
      case 'editRes': S.editRes = S.residents.find(x => x.id === id); return tabResidents();
      case 'cancelRes': S.editRes = null; return tabResidents();
      case 'delRes': if (!confirm('Delete this resident along with their ratings?')) return;
        await api(`/api/residents/${id}`, { method: 'DELETE' }); toast('Resident deleted'); S.editRes = null; return refresh();
    }
  } catch (err) { toast(err.message, true); }
});

document.addEventListener('submit', async e => {
  e.preventDefault();
  if (e.target.id === 'rateForm') return saveRating();
  try {
    if (e.target.id === 'mealForm') {
      const body = { date: S.draft.date, mealType: S.draft.mealType, items: S.draft.items };
      try {
        if (S.editMeal) await api(`/api/meals/${S.editMeal.id}`, { method: 'PUT', body });
        else await api('/api/meals', { method: 'POST', body });
      } catch (err) { $('#mealErr').textContent = err.message; return; }
      toast(S.editMeal ? 'Meal updated' : 'Meal published');
      S.week = mondayOf(parse(S.draft.date)); blankDraft(); refresh();
    }
    if (e.target.id === 'resForm') {
      const body = { name: $('#rn').value, roomNo: $('#rr').value, email: $('#re').value };
      try {
        if (S.editRes) await api(`/api/residents/${S.editRes.id}`, { method: 'PUT', body });
        else await api('/api/residents', { method: 'POST', body });
      } catch (err) { $('#resErr').textContent = err.message; return; }
      toast(S.editRes ? 'Resident updated' : 'Resident added'); S.editRes = null; refresh();
    }
  } catch (err) { toast(err.message, true); }
});

document.addEventListener('input', e => {
  const t = e.target;
  if (t.dataset.f) S.draft.items[+t.dataset.i][t.dataset.f] = t.type === 'checkbox' ? t.checked : t.value;
  if (t.id === 'md') S.draft.date = t.value;
  if (t.id === 'sq') runSearch();
});
document.addEventListener('change', e => {
  const t = e.target;
  if (t.id === 'who') { S.rid = t.value; localStorage.setItem('mm-rid', S.rid); refresh(); }
  if (t.id === 'mt') S.draft.mealType = t.value;
  if (t.id === 'sc') runSearch();
  if (t.id === 'hcd') { S.hcDate = t.value; if (t.value) renderCommittee(); }
  if (t.id === 'th') { S.threshold = Math.min(5, Math.max(1, +t.value || 3)); renderCommittee(); }
});

refresh();
