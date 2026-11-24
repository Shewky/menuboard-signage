package com.anypay.menuboard

object WebPanelHtml {
    fun getManifestJson(): String = """
{
  "name": "İnterfon Menuboard Kumanda",
  "short_name": "Menuboard",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#0f172a",
  "theme_color": "#1e293b",
  "icons": [
    {
      "src": "/api/logo",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/api/logo",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
""".trimIndent()

    fun getHtml(): String = """
<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<meta name="theme-color" content="#1e293b">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-title" content="Menuboard">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<link rel="manifest" href="/manifest.json">
<link rel="icon" type="image/png" href="/api/logo">
<link rel="apple-touch-icon" href="/api/logo">
<title>Menuboard Web Kumanda</title>
<style>
*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:#0f172a;color:#f1f5f9;min-height:100vh;padding-bottom:76px}
.hidden{display:none!important}
.login-wrap{position:fixed;inset:0;background:#0f172a;z-index:999;display:flex;align-items:center;justify-content:center;padding:24px}
.login-card{width:100%;max-width:360px;background:#1e293b;border:1px solid #334155;border-radius:16px;padding:28px 22px;text-align:center;box-shadow:0 20px 50px rgba(0,0,0,.5)}
.login-logo{max-height:54px;max-width:220px;object-fit:contain;margin-bottom:14px}
.login-card h1{font-size:19px;margin-bottom:6px;color:#38bdf8}
.login-card p{font-size:13px;color:#94a3b8;margin-bottom:18px}
.topbar{position:sticky;top:0;z-index:50;background:#1e293b;border-bottom:1px solid #334155;padding:10px 14px;display:flex;align-items:center;justify-content:space-between;gap:8px}
.brand-wrap{display:flex;align-items:center;gap:10px;min-width:0}
.top-logo{height:30px;max-width:96px;object-fit:contain;flex-shrink:0}
.brand{display:flex;flex-direction:column;min-width:0}
.brand-title{font-size:14px;font-weight:700;color:#fff;display:flex;align-items:center;gap:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.dot{width:9px;height:9px;border-radius:50%;background:#22c55e;display:inline-block;flex-shrink:0}
.brand-sub{font-size:11px;color:#94a3b8}
.top-actions{display:flex;gap:6px;flex-shrink:0}
.icon-btn{background:#334155;color:#f8fafc;border:none;border-radius:10px;padding:8px 10px;font-size:13px;font-weight:600;cursor:pointer}
.icon-btn.warn{background:#f59e0b;color:#111}
.icon-btn.danger{background:#dc2626;color:#fff}
.container{max-width:720px;margin:0 auto;padding:14px}
.btn-row{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-bottom:14px}
.btn{border:none;border-radius:10px;padding:11px 10px;font-size:13px;font-weight:700;color:#fff;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:5px;text-align:center}
.btn-blue{background:#0284c7}.btn-teal{background:#0d9488}.btn-pink{background:#db2777}
.btn-orange{background:#ea580c}.btn-red{background:#dc2626}.btn-slate{background:#334155}
.btn-green{background:#16a34a}.btn-full{width:100%}
.card{background:#1e293b;border:1px solid #334155;border-radius:14px;padding:14px;margin-bottom:12px}
.card-title{font-size:14px;font-weight:700;color:#38bdf8;margin-bottom:10px}
.media-item{display:flex;align-items:center;gap:10px;background:#1e293b;border:1px solid #334155;border-radius:12px;padding:10px;margin-bottom:9px}
.media-item.is-hidden{opacity:.45}
.thumb{width:54px;height:54px;border-radius:8px;object-fit:cover;background:#0f172a;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:22px}
.media-info{flex:1;min-width:0}
.media-name{font-size:13px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.media-meta{font-size:11px;color:#94a3b8;margin-top:3px}
.badge{display:inline-block;font-size:10px;font-weight:700;padding:2px 6px;border-radius:5px;margin-right:4px;color:#fff}
.b-img{background:#16a34a}.b-vid{background:#2563eb}.b-web{background:#0d9488}.b-ig{background:#db2777}
.media-btns{display:flex;gap:4px;flex-wrap:wrap;justify-content:flex-end}
.mini-btn{background:#334155;color:#fff;border:none;border-radius:7px;padding:6px 8px;font-size:12px;cursor:pointer}
.mini-btn.del{background:#991b1b}
.field{margin-bottom:11px;text-align:left}
.field label{display:block;font-size:12px;color:#cbd5e1;margin-bottom:4px;font-weight:600}
.input,select{width:100%;background:#0f172a;color:#fff;border:1px solid #475569;border-radius:9px;padding:10px 12px;font-size:14px;outline:none}
.input:focus,select:focus{border-color:#38bdf8}
.check-row{display:flex;align-items:center;gap:8px;margin:8px 0;font-size:13px;cursor:pointer}
.check-row input{width:18px;height:18px;accent-color:#38bdf8}
.switch-row{display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #334155;gap:10px}
.switch-row:last-child{border-bottom:none}
.bottom-nav{position:fixed;bottom:0;left:0;right:0;background:#1e293b;border-top:1px solid #334155;display:grid;grid-template-columns:repeat(3,1fr);z-index:60}
.nav-tab{background:none;border:none;color:#94a3b8;padding:10px 4px;font-size:11px;font-weight:700;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:3px}
.nav-tab.active{color:#38bdf8}
.nav-tab span{font-size:18px}
.modal-bg{position:fixed;inset:0;background:rgba(0,0,0,.75);z-index:200;display:flex;align-items:flex-end;justify-content:center;padding:10px}
.modal{width:100%;max-width:520px;max-height:90vh;overflow-y:auto;background:#1e293b;border:1px solid #475569;border-radius:16px;padding:18px}
.modal-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.modal-head h3{font-size:16px;color:#38bdf8}
.toast{position:fixed;top:66px;left:50%;transform:translateX(-50%);background:#0284c7;color:#fff;padding:10px 16px;border-radius:10px;font-size:13px;font-weight:600;z-index:9999;box-shadow:0 8px 24px rgba(0,0,0,.4);opacity:0;pointer-events:none;transition:opacity .25s}
.toast.show{opacity:1}
.prog-wrap{width:100%;height:8px;background:#0f172a;border-radius:6px;overflow:hidden;margin:10px 0}
.prog-bar{height:100%;width:0%;background:#22c55e;transition:width .15s}
.peer-item{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:11px;background:#0f172a;border:1px solid #334155;border-radius:10px;margin-bottom:8px}
.app-list{max-height:320px;overflow-y:auto}
.app-row{display:flex;justify-content:space-between;align-items:center;padding:9px 6px;border-bottom:1px solid #334155;gap:8px}
input[type=range]{width:100%;accent-color:#38bdf8}
</style>
</head>
<body>

<div id="toast" class="toast"></div>

<div id="loginScreen" class="login-wrap">
  <div class="login-card">
    <img src="/api/logo" class="login-logo" alt="İnterfon" onerror="this.style.display='none'">
    <h1>Menuboard Panel</h1>
    <p>Yönetim paneline erişmek için şifreyi girin</p>
    <div class="field">
      <input id="loginPass" type="password" class="input" placeholder="Şifre" autocomplete="current-password">
    </div>
    <button class="btn btn-blue btn-full" onclick="doLogin()">🔓 Giriş Yap</button>
    <div id="loginErr" style="color:#f87171;font-size:12px;margin-top:10px"></div>
  </div>
</div>

<div id="appShell" class="hidden">
  <div class="topbar">
    <div class="brand-wrap">
      <img src="/api/logo" class="top-logo" alt="İnterfon" onerror="this.style.display='none'">
      <div class="brand">
        <div class="brand-title"><span class="dot"></span><span id="hdrTag">Menuboard</span> <span id="hdrVer" style="font-size:11px;color:#38bdf8"></span></div>
        <div class="brand-sub" id="hdrHost">Yerel Ağ</div>
      </div>
    </div>
    <div class="top-actions">
      <button id="btnFind" class="icon-btn warn" onclick="toggleFind()">Bul</button>
      <button class="icon-btn" onclick="editTag()">✏️</button>
      <button class="icon-btn danger" onclick="doLogout()" title="Çıkış">🔒</button>
    </div>
  </div>

  <div class="container">
    <!-- TAB 1: MEDYA -->
    <div id="tabMedia">
      <div class="btn-row">
        <button class="btn btn-blue" onclick="openUploadModal()">📁 Medya</button>
        <button class="btn btn-teal" onclick="openUrlModal(null)">🌐 URL</button>
        <button class="btn btn-pink" onclick="openIgModal(null)">📸 Instagram</button>
      </div>
      <div id="playlistBox"></div>
    </div>

    <!-- TAB 2: AYARLAR -->
    <div id="tabSettings" class="hidden">
      <div class="card">
        <div class="card-title">🖥️ Ekran ve Sistem</div>
        <div class="switch-row">
          <div><b>Ekran Yönü</b><div style="font-size:11px;color:#94a3b8" id="txtOrientation">Dikey (9:16)</div></div>
          <button class="btn btn-blue" onclick="toggleOrientation()">🔄 Değiştir</button>
        </div>
        <div class="switch-row">
          <div><b>Tam Ekran (Çubuklar Gizli)</b><div style="font-size:11px;color:#94a3b8" id="txtImmersive">Açık</div></div>
          <button class="btn btn-slate" onclick="toggleImmersive()">Değiştir</button>
        </div>
        <div class="switch-row">
          <div><b>Açılışta Otomatik Başlat</b><div style="font-size:11px;color:#94a3b8" id="txtAutoStart">Kapalı</div></div>
          <button class="btn btn-slate" onclick="toggleAutoStart()">Değiştir</button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">🌫️ Bulanık Arka Plan (Blur Dolgu)</div>
        <label class="check-row"><input id="cbBlur" type="checkbox"> Bulanık Arka Planı Etkinleştir</label>
        <div class="field">
          <label>Koyuluk / Karartma: <span id="lblDark">30</span>%</label>
          <input id="rngDark" type="range" min="0" max="85" value="30" oninput="document.getElementById('lblDark').textContent=this.value">
        </div>
        <div class="field">
          <label>Bulanıklık Şiddeti</label>
          <select id="selBlurInt"><option value="0">Az</option><option value="1" selected>Orta</option><option value="2">Çok</option></select>
        </div>
        <div class="field">
          <label>Arka Plan Kalitesi</label>
          <select id="selBlurQual"><option value="0">Düşük (En Hafif)</option><option value="1" selected>Orta (Dengeli)</option><option value="2">Yüksek (En Net)</option></select>
        </div>
        <label class="check-row"><input id="cbBlurIg" type="checkbox"> Instagram medyalarını da etkilesin</label>
        <button class="btn btn-blue btn-full" onclick="saveBlur()">🌫️ Bulanık Arka Planı Uygula</button>
      </div>

      <div class="card">
        <div class="card-title">🛠️ Cihaz Araçları</div>
        <div style="display:grid;gap:8px">
          <button class="btn btn-slate btn-full" onclick="sendInput('type=nav&act=HOME_SETTINGS');toast('Launcher ayarları açılıyor...')">🏠 Varsayılan Ana Ekran (Launcher) Ayarını Aç</button>
          <button class="btn btn-blue btn-full" onclick="openAppsModal()">🚀 Kurulu Uygulamaları Gör / Başlat</button>
          <button class="btn btn-teal btn-full" onclick="openApkModal()">📦 Uzaktan APK Gönder & Kur</button>
          <button class="btn btn-slate btn-full" onclick="checkRemoteUpdate()">⬇️ Güncelleme Kontrol Et</button>
          <button class="btn btn-orange btn-full" onclick="sendInput('type=nav&act=MENUBOARD');toast('Menuboard öne getiriliyor...')">📺 Menuboard'u Öne Getir</button>
          <button class="btn btn-red btn-full" onclick="restartDevice()">🔄 Menuboard Uygulamasını Restart Et</button>
        </div>
      </div>
    </div>

    <!-- TAB 3: CİHAZLAR -->
    <div id="tabPeers" class="hidden">
      <div class="card">
        <div class="card-title" style="display:flex;justify-content:space-between;align-items:center">
          <span>📡 Ağdaki Menuboardlar</span>
          <button class="mini-btn" onclick="loadPeers()">🔄 Yenile</button>
        </div>
        <div id="peersBox"></div>
      </div>
    </div>
  </div>

  <div class="bottom-nav">
    <button class="nav-tab active" id="nav0" onclick="switchTab(0)"><span>📁</span>Medya</button>
    <button class="nav-tab" id="nav1" onclick="switchTab(1)"><span>⚙️</span>Ayarlar</button>
    <button class="nav-tab" id="nav2" onclick="switchTab(2)"><span>📡</span>Cihazlar</button>
  </div>
</div>

<div id="modalBg" class="modal-bg hidden" onclick="if(event.target===this)closeModal()">
  <div class="modal" id="modalContent"></div>
</div>

<script>
var state = { info: {}, playlist: [], finding: false };
var ANIM_OPTS = [
  ['fade','Solma (Fade)'],['slide_left','Soldan Kay'],['slide_right','Sağdan Kay'],
  ['slide_up','Yukarıdan Kay'],['slide_down','Aşağıdan Kay'],['zoom_in','Yakınlaş'],
  ['zoom_out','Uzaklaş'],['none','Animasyonsuz']
];
var SPEED_OPTS = [['slow','Yavaş'],['slow_medium','Yavaş - Orta'],['medium','Orta'],['medium_fast','Orta - Hızlı'],['fast','Hızlı']];
var FONT_OPTS = [
  ['default','Site Varsayılanı'],['Roboto','Roboto'],['Open Sans','Open Sans'],['Montserrat','Montserrat'],
  ['Poppins','Poppins'],['Lato','Lato'],['Oswald','Oswald'],['Playfair Display','Playfair Display'],
  ['Merriweather','Merriweather'],['sans-serif','Sistem Sans-Serif'],['serif','Sistem Serif'],
  ['monospace','Monospace'],['cursive','El Yazısı']
];

function toast(msg) {
  var el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(window._tTimer);
  window._tTimer = setTimeout(function(){ el.classList.remove('show'); }, 2600);
}

function esc(s) {
  return String(s == null ? '' : s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function optsHtml(list, sel) {
  var h = '';
  for (var i = 0; i < list.length; i++) {
    h += '<option value="' + list[i][0] + '"' + (String(list[i][0]) === String(sel) ? ' selected' : '') + '>' + list[i][1] + '</option>';
  }
  return h;
}

function doLogin() {
  var pass = document.getElementById('loginPass').value;
  fetch('/api/web/login?pass=' + encodeURIComponent(pass), { method: 'POST' })
    .then(function(r){ return r.json(); })
    .then(function(d){
      if (d.ok) {
        localStorage.setItem('mb_web_auth', 'ok_152');
        showApp();
      } else {
        document.getElementById('loginErr').textContent = 'Hatalı şifre!';
      }
    }).catch(function(){ document.getElementById('loginErr').textContent = 'Bağlantı hatası!'; });
}

function doLogout() {
  localStorage.removeItem('mb_web_auth');
  document.getElementById('appShell').classList.add('hidden');
  document.getElementById('loginScreen').classList.remove('hidden');
  document.getElementById('loginPass').value = '';
}

function showApp() {
  document.getElementById('loginScreen').classList.add('hidden');
  document.getElementById('appShell').classList.remove('hidden');
  document.getElementById('hdrHost').textContent = location.host;
  loadPing();
  loadPlaylist();
  loadPeers();
}

function switchTab(idx) {
  var tabs = ['tabMedia','tabSettings','tabPeers'];
  for (var i = 0; i < tabs.length; i++) {
    document.getElementById(tabs[i]).classList.toggle('hidden', i !== idx);
    document.getElementById('nav' + i).classList.toggle('active', i === idx);
  }
  if (idx === 2) loadPeers();
  if (idx === 1) loadPing();
}

function loadPing() {
  fetch('/api/ping').then(function(r){ return r.json(); }).then(function(d){
    state.info = d;
    document.getElementById('hdrTag').textContent = d.tag || 'Menuboard';
    document.getElementById('hdrVer').textContent = d.version ? ('v' + d.version) : '';
    document.getElementById('txtOrientation').textContent = d.orientation === 'landscape' ? 'TV Modu (16:9 Yatay)' : 'Dikey Mod (9:16)';
    document.getElementById('txtImmersive').textContent = d.immersive !== 'false' ? 'Açık (Çubuklar Gizli)' : 'Kapalı (Çubuklar Görünür)';
    document.getElementById('txtAutoStart').textContent = d.autoStart === 'true' ? 'Açık' : 'Kapalı';
    document.getElementById('cbBlur').checked = d.blurBg === 'true';
    var dk = parseInt(d.blurDarkness || '30', 10);
    document.getElementById('rngDark').value = dk;
    document.getElementById('lblDark').textContent = dk;
    document.getElementById('selBlurInt').value = d.blurIntensity || '1';
    document.getElementById('selBlurQual').value = d.blurQuality || '1';
    document.getElementById('cbBlurIg').checked = d.blurIg === 'true';
  }).catch(function(){});
}

function loadPlaylist() {
  fetch('/api/playlist').then(function(r){ return r.json(); }).then(function(list){
    state.playlist = Array.isArray(list) ? list : [];
    renderPlaylist();
  }).catch(function(){});
}

function syncPlaylist(msg) {
  fetch('/api/playlist', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(state.playlist)
  }).then(function(r){
    if (r.ok) { toast(msg || 'Menuboard güncellendi!'); loadPlaylist(); }
    else toast('Kaydetme hatası!');
  }).catch(function(){ toast('Bağlantı hatası!'); });
}

function renderPlaylist() {
  var box = document.getElementById('playlistBox');
  if (!state.playlist.length) {
    box.innerHTML = '<div class="card" style="text-align:center;color:#94a3b8;padding:30px">Henüz medya eklenmedi.<br>Yukarıdaki butonlardan Fotoğraf, Video, URL veya Instagram ekleyin.</div>';
    return;
  }
  var html = '';
  for (var i = 0; i < state.playlist.length; i++) {
    var it = state.playlist[i];
    var badge = '<span class="badge b-img">RESİM</span>';
    var thumb = '<img class="thumb" src="/api/thumbnail?filename=' + encodeURIComponent(it.fileName) + '" onerror="this.style.display=\'none\'">';
    var meta = (it.durationSec > 0 ? it.durationSec : 10) + 'sn | +' + (it.waitAfterSec || 0) + 'sn ara';
    if (it.type === 'video') {
      badge = '<span class="badge b-vid">VİDEO</span>';
      meta = (it.durationSec > 0 ? (it.durationSec + 'sn kesit') : 'Tamamı') + ' | +' + (it.waitAfterSec || 0) + 'sn ara';
    } else if (it.type === 'web') {
      badge = '<span class="badge b-web">URL</span>';
      thumb = '<div class="thumb">🌐</div>';
      meta = (it.scrollUntilEnd ? 'Sonuna Kadar' : ((it.durationSec || 20) + 'sn')) + ' | Zoom:%' + (it.webZoomPercent || 100) + (it.showQr ? ' | QR' : '');
    } else if (it.type === 'instagram') {
      badge = '<span class="badge b-ig">IG</span>';
      thumb = '<div class="thumb">📸</div>';
      meta = (it.scrollUntilEnd ? 'Tümü Bitene Kadar' : ((it.durationSec || 60) + 'sn')) + (it.igAudioEnabled ? ' | Sesli' : '');
    }
    html += '<div class="media-item' + (it.isHidden ? ' is-hidden' : '') + '">' + thumb +
      '<div class="media-info"><div class="media-name">' + badge + esc(it.fileName) + (it.isHidden ? ' (Gizli)' : '') + '</div>' +
      '<div class="media-meta">' + esc(meta) + '</div></div>' +
      '<div class="media-btns">' +
      (i > 0 ? '<button class="mini-btn" onclick="moveItem(' + i + ',-1)">▲</button>' : '') +
      (i < state.playlist.length - 1 ? '<button class="mini-btn" onclick="moveItem(' + i + ',1)">▼</button>' : '') +
      '<button class="mini-btn" onclick="toggleHide(' + i + ')">' + (it.isHidden ? '🙈' : '👁️') + '</button>' +
      '<button class="mini-btn" onclick="editItem(' + i + ')">✏️</button>' +
      '<button class="mini-btn del" onclick="delItem(' + i + ')">🗑️</button>' +
      '</div></div>';
  }
  box.innerHTML = html;
}

function moveItem(i, dir) {
  var j = i + dir;
  if (j < 0 || j >= state.playlist.length) return;
  var tmp = state.playlist[i]; state.playlist[i] = state.playlist[j]; state.playlist[j] = tmp;
  syncPlaylist('Sıralama güncellendi');
}

function toggleHide(i) {
  state.playlist[i].isHidden = !state.playlist[i].isHidden;
  syncPlaylist(state.playlist[i].isHidden ? 'Medya gizlendi' : 'Medya görünür yapıldı');
}

function delItem(i) {
  if (!confirm('"' + state.playlist[i].fileName + '" silinsin mi?')) return;
  state.playlist.splice(i, 1);
  syncPlaylist('Öğe silindi');
}

function editItem(i) {
  var it = state.playlist[i];
  if (it.type === 'web') openUrlModal(i);
  else if (it.type === 'instagram') openIgModal(i);
  else openEditMediaModal(i);
}

function openModal(html) {
  document.getElementById('modalContent').innerHTML = html;
  document.getElementById('modalBg').classList.remove('hidden');
}
function closeModal() { document.getElementById('modalBg').classList.add('hidden'); }

function openUploadModal() {
  openModal(
    '<div class="modal-head"><h3>📁 Fotoğraf / Video Yükle</h3><button class="mini-btn" onclick="closeModal()">✖</button></div>' +
    '<div class="field"><label>Dosya Seç (Resim veya Video)</label><input id="upFile" type="file" accept="image/*,video/*" class="input" onchange="onPickFile(this)"></div>' +
    '<div class="field"><label>Oynatma Süresi (sn) — Video için 0 = Tamamı</label><input id="upDur" type="number" class="input" value="10"></div>' +
    '<div class="field"><label>Ara Bekleme (sn)</label><input id="upWait" type="number" class="input" value="0"></div>' +
    '<div class="field"><label>Giriş Animasyonu</label><select id="upAnim">' + optsHtml(ANIM_OPTS, 'fade') + '</select></div>' +
    '<div id="upProgWrap" class="prog-wrap hidden"><div id="upProgBar" class="prog-bar"></div></div>' +
    '<button id="btnDoUp" class="btn btn-blue btn-full" onclick="doUploadMedia()">⬆️ Yükle ve Yayınla</button>'
  );
}

function onPickFile(inp) {
  if (inp.files && inp.files[0]) {
    var isVid = (inp.files[0].type || '').indexOf('video') === 0;
    document.getElementById('upDur').value = isVid ? '0' : '10';
  }
}

function doUploadMedia() {
  var f = document.getElementById('upFile').files[0];
  if (!f) { toast('Lütfen önce dosya seçin!'); return; }
  var isVid = (f.type || '').indexOf('video') === 0;
  var safeName = f.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  var dur = parseInt(document.getElementById('upDur').value || '0', 10);
  var wait = parseInt(document.getElementById('upWait').value || '0', 10);
  var anim = document.getElementById('upAnim').value;
  var btn = document.getElementById('btnDoUp');
  btn.disabled = true; btn.textContent = 'Yükleniyor...';
  document.getElementById('upProgWrap').classList.remove('hidden');

  var xhr = new XMLHttpRequest();
  xhr.open('POST', '/api/upload_raw?filename=' + encodeURIComponent(safeName), true);
  xhr.upload.onprogress = function(e) {
    if (e.lengthComputable) document.getElementById('upProgBar').style.width = Math.round((e.loaded / e.total) * 100) + '%';
  };
  xhr.onload = function() {
    if (xhr.status === 200) {
      state.playlist.push({ id: String(Date.now()), fileName: safeName, type: isVid ? 'video' : 'image', durationSec: dur, waitAfterSec: wait, animation: anim, isHidden: false });
      closeModal();
      syncPlaylist('Medya yüklendi ve yayınlandı!');
    } else { btn.disabled = false; btn.textContent = '⬆️ Yükle ve Yayınla'; toast('Yükleme başarısız!'); }
  };
  xhr.onerror = function() { btn.disabled = false; btn.textContent = '⬆️ Yükle ve Yayınla'; toast('Bağlantı hatası!'); };
  xhr.send(f);
}

function openEditMediaModal(idx) {
  var it = state.playlist[idx];
  openModal(
    '<div class="modal-head"><h3>✏️ Medya Ayarları</h3><button class="mini-btn" onclick="closeModal()">✖</button></div>' +
    '<div class="field"><label>Oynatma Süresi (sn)</label><input id="edDur" type="number" class="input" value="' + (it.durationSec || 0) + '"></div>' +
    '<div class="field"><label>Ara Bekleme (sn)</label><input id="edWait" type="number" class="input" value="' + (it.waitAfterSec || 0) + '"></div>' +
    '<div class="field"><label>Giriş Animasyonu</label><select id="edAnim">' + optsHtml(ANIM_OPTS, it.animation || 'fade') + '</select></div>' +
    '<button class="btn btn-green btn-full" onclick="saveMediaEdit(' + idx + ')">💾 Kaydet</button>'
  );
}

function saveMediaEdit(idx) {
  var it = state.playlist[idx];
  it.durationSec = parseInt(document.getElementById('edDur').value || '0', 10);
  it.waitAfterSec = parseInt(document.getElementById('edWait').value || '0', 10);
  it.animation = document.getElementById('edAnim').value;
  closeModal();
  syncPlaylist('Medya ayarları kaydedildi');
}

function openUrlModal(idx) {
  var it = idx != null ? state.playlist[idx] : {};
  var zoomOpts = [[50,'%50'],[75,'%75'],[80,'%80'],[90,'%90'],[100,'%100 (Normal)'],[110,'%110'],[125,'%125'],[150,'%150'],[175,'%175'],[200,'%200']];
  var cornerOpts = [['bottom_right','Sağ Alt'],['bottom_left','Sol Alt'],['top_right','Sağ Üst'],['top_left','Sol Üst']];
  openModal(
    '<div class="modal-head"><h3>🌐 ' + (idx != null ? 'URL Düzenle' : 'Yeni URL Ekle') + '</h3><button class="mini-btn" onclick="closeModal()">✖</button></div>' +
    '<div class="field"><label>Web Adresi (URL)</label><input id="uUrl" type="url" class="input" placeholder="https://..." value="' + esc(it.fileName || '') + '"></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' +
      '<div class="field"><label>Süre (sn)</label><input id="uDur" type="number" class="input" value="' + (it.durationSec || 20) + '"></div>' +
      '<div class="field"><label>Ara Bekleme (sn)</label><input id="uWait" type="number" class="input" value="' + (it.waitAfterSec || 0) + '"></div>' +
    '</div>' +
    '<div class="field"><label>Giriş Animasyonu</label><select id="uAnim">' + optsHtml(ANIM_OPTS, it.animation || 'fade') + '</select></div>' +
    '<label class="check-row"><input id="uAuto" type="checkbox"' + (it.autoScroll !== false ? ' checked' : '') + '> Otomatik Aşağı Kaydır</label>' +
    '<label class="check-row"><input id="uEnd" type="checkbox"' + (it.scrollUntilEnd ? ' checked' : '') + '> Sayfa Bitimine Kadar Kaydır</label>' +
    '<div class="field"><label>Kaydırma Hızı</label><select id="uSpd">' + optsHtml(SPEED_OPTS, it.webScrollSpeed || 'medium') + '</select></div>' +
    '<label class="check-row"><input id="uFix" type="checkbox"' + (it.webFixedHeader ? ' checked' : '') + '> Üst başlık / menü çubuğu sabit kalsın</label>' +
    '<label class="check-row"><input id="uFixTxtEn" type="checkbox"' + (it.webFixedHeaderTextEnabled ? ' checked' : '') + '> Sabit üst metin ayarla</label>' +
    '<div class="field"><input id="uFixTxt" maxlength="50" class="input" placeholder="Sabit üst metin (maks. 50 krk)" value="' + esc(it.webFixedHeaderText || '') + '"></div>' +
    '<div class="field"><input id="uFixSel" class="input" placeholder="Özel CSS Seçici (Boş = Otomatik)" value="' + esc(it.webFixedSelector || '') + '"></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' +
      '<div class="field"><label>Zoom</label><select id="uZoom">' + optsHtml(zoomOpts, it.webZoomPercent || 100) + '</select></div>' +
      '<div class="field"><label>Yazı Tipi</label><select id="uFont">' + optsHtml(FONT_OPTS, it.webFontFamily || 'default') + '</select></div>' +
    '</div>' +
    '<label class="check-row"><input id="uBold" type="checkbox"' + (it.webBoldHeaders ? ' checked' : '') + '> Başlıkları Kalın (Bold) Yap</label>' +
    '<label class="check-row"><input id="uQr" type="checkbox"' + (it.showQr ? ' checked' : '') + '> Sayfa Linkini QR Kod Olarak Göster</label>' +
    '<div class="field"><label>QR Köşesi</label><select id="uQrC">' + optsHtml(cornerOpts, it.qrCorner || 'bottom_right') + '</select></div>' +
    '<button class="btn btn-teal btn-full" onclick="saveUrl(' + (idx != null ? idx : -1) + ')">💾 Kaydet ve Yayınla</button>'
  );
}

function saveUrl(idx) {
  var u = document.getElementById('uUrl').value.trim();
  if (!u) { toast('Lütfen geçerli bir URL girin!'); return; }
  if (u.indexOf('http://') !== 0 && u.indexOf('https://') !== 0) u = 'https://' + u;
  var fix = document.getElementById('uFix').checked;
  var obj = {
    id: idx >= 0 ? state.playlist[idx].id : String(Date.now()),
    fileName: u, type: 'web',
    durationSec: parseInt(document.getElementById('uDur').value || '20', 10),
    waitAfterSec: parseInt(document.getElementById('uWait').value || '0', 10),
    animation: document.getElementById('uAnim').value,
    autoScroll: document.getElementById('uAuto').checked || document.getElementById('uEnd').checked,
    scrollUntilEnd: document.getElementById('uEnd').checked,
    webScrollSpeed: document.getElementById('uSpd').value,
    webFixedHeader: fix,
    webFixedHeaderTextEnabled: fix && document.getElementById('uFixTxtEn').checked,
    webFixedHeaderText: document.getElementById('uFixTxt').value.trim().slice(0, 50),
    webFixedSelector: document.getElementById('uFixSel').value.trim(),
    webZoomPercent: parseInt(document.getElementById('uZoom').value || '100', 10),
    webFontFamily: document.getElementById('uFont').value,
    webFontSizePercent: 0, webImageScalePercent: 100,
    webBoldHeaders: document.getElementById('uBold').checked,
    showQr: document.getElementById('uQr').checked,
    qrCorner: document.getElementById('uQrC').value,
    qrSizeDp: 150, qrBorderDp: 8,
    isHidden: idx >= 0 ? !!state.playlist[idx].isHidden : false
  };
  if (idx >= 0) state.playlist[idx] = obj; else state.playlist.push(obj);
  closeModal();
  syncPlaylist('URL kaydedildi');
}

function openIgModal(idx) {
  fetch('/api/instagram/status').then(function(r){ return r.json(); }).then(function(st){
    var it = idx != null ? state.playlist[idx] : {};
    var active = !!st.active;
    var fltOpts = [['all','Tümü (Fotoğraf + Video)'],['photo','Sadece Fotoğraflar'],['video','Sadece Videolar']];
    openModal(
      '<div class="modal-head"><h3>📸 ' + (idx != null ? 'Instagram Düzenle' : 'Instagram Ekle') + '</h3><button class="mini-btn" onclick="closeModal()">✖</button></div>' +
      '<div style="padding:10px;border-radius:9px;margin-bottom:12px;font-size:12px;font-weight:700;background:' + (active ? '#14532d;color:#86efac' : '#7f1d1d;color:#fecaca') + '">' +
        (active ? '🟢 Menuboard Instagram Oturumu: AKTİF' : '🔴 Oturum Yok — İlk Instagram girişini Android kumandadan bir kez yapın.') +
      '</div>' +
      '<div class="field"><label>Instagram Kullanıcı Adı</label><input id="igUser" class="input" placeholder="ornekrestoran" value="' + esc(it.fileName || '') + '"></div>' +
      '<div class="field"><label>Medya Filtresi</label><select id="igFlt">' + optsHtml(fltOpts, it.igMediaFilter || 'all') + '</select></div>' +
      '<label class="check-row"><input id="igEnd" type="checkbox"' + (it.scrollUntilEnd !== false ? ' checked' : '') + '> Tüm medyalar bitene kadar oynat</label>' +
      '<label class="check-row"><input id="igAud" type="checkbox"' + (it.igAudioEnabled ? ' checked' : '') + '> 🔊 Videoları Sesli Oynat</label>' +
      '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' +
        '<div class="field"><label>Toplam Süre (sn)</label><input id="igDur" type="number" class="input" value="' + (it.durationSec || 60) + '"></div>' +
        '<div class="field"><label>Fotoğraf Süresi (sn)</label><input id="igSld" type="number" class="input" value="' + (it.igSlideSec || 8) + '"></div>' +
      '</div>' +
      '<div class="field"><label>Animasyon</label><select id="igAnim">' + optsHtml(ANIM_OPTS, it.animation || 'fade') + '</select></div>' +
      '<button class="btn btn-pink btn-full" onclick="saveIg(' + (idx != null ? idx : -1) + ')">💾 Kaydet ve Yayınla</button>'
    );
  }).catch(function(){ toast('Instagram durumu alınamadı!'); });
}

function saveIg(idx) {
  var u = document.getElementById('igUser').value.trim();
  if (!u) { toast('Kullanıcı adı girin!'); return; }
  var obj = {
    id: idx >= 0 ? state.playlist[idx].id : String(Date.now()),
    fileName: u, type: 'instagram',
    durationSec: parseInt(document.getElementById('igDur').value || '60', 10),
    waitAfterSec: 0, animation: document.getElementById('igAnim').value,
    scrollUntilEnd: document.getElementById('igEnd').checked,
    igMediaFilter: document.getElementById('igFlt').value,
    igSlideSec: parseInt(document.getElementById('igSld').value || '8', 10),
    igAudioEnabled: document.getElementById('igAud').checked,
    isHidden: idx >= 0 ? !!state.playlist[idx].isHidden : false
  };
  if (idx >= 0) state.playlist[idx] = obj; else state.playlist.push(obj);
  closeModal();
  syncPlaylist('Instagram kaydedildi');
}

function toggleFind() {
  state.finding = !state.finding;
  var b = document.getElementById('btnFind');
  b.textContent = state.finding ? 'Gizle' : 'Bul';
  b.className = state.finding ? 'icon-btn danger' : 'icon-btn warn';
  fetch('/api/identify?show=' + state.finding + '&tag=' + encodeURIComponent(state.info.tag || 'Menuboard'), { method: 'POST' });
}

function editTag() {
  var cur = state.info.tag || 'Menuboard';
  var nt = prompt('Cihaz Nametag (Etiket) Belirle:', cur);
  if (nt && nt.trim()) {
    fetch('/api/tag?tag=' + encodeURIComponent(nt.trim()), { method: 'POST' }).then(function(){
      toast('Etiket güncellendi'); loadPing();
    });
  }
}

function toggleOrientation() {
  var next = state.info.orientation === 'landscape' ? 'portrait' : 'landscape';
  fetch('/api/orientation?mode=' + next, { method: 'POST' }).then(function(){
    toast(next === 'landscape' ? '📺 TV Modu (Yatay) aktif' : '📱 Dikey Mod aktif'); loadPing();
  });
}

function toggleImmersive() {
  var next = state.info.immersive === 'false';
  fetch('/api/immersive?enabled=' + next, { method: 'POST' }).then(function(){
    toast(next ? '🖥️ Tam ekran açıldı' : '📱 Sistem çubukları görünür'); loadPing();
  });
}

function toggleAutoStart() {
  var next = state.info.autoStart !== 'true';
  fetch('/api/autostart?enabled=' + next, { method: 'POST' }).then(function(){
    toast(next ? '🚀 Açılışta otomatik başlatma AÇIK' : '⏹️ Açılışta otomatik başlatma KAPALI'); loadPing();
  });
}

function saveBlur() {
  var en = document.getElementById('cbBlur').checked;
  var dk = document.getElementById('rngDark').value;
  var it = document.getElementById('selBlurInt').value;
  var ql = document.getElementById('selBlurQual').value;
  var ig = document.getElementById('cbBlurIg').checked;
  fetch('/api/blur_bg?enabled=' + en + '&darkness=' + dk + '&intensity=' + it + '&quality=' + ql + '&ig=' + ig, { method: 'POST' })
    .then(function(){ toast('Bulanık arka plan güncellendi'); loadPing(); });
}

function checkRemoteUpdate() {
  fetch('/api/update/check', { method: 'POST' }).then(function(){ toast('Güncelleme kontrolü başlatıldı!'); });
}

function restartDevice() {
  if (!confirm('Menuboard uygulaması yeniden başlatılsın mı?')) return;
  fetch('/api/restart', { method: 'POST' }).then(function(){ toast('🔄 Menuboard yeniden başlatılıyor...'); });
}

function openAppsModal() {
  toast('Uygulamalar getiriliyor...');
  fetch('/api/apps').then(function(r){ return r.json(); }).then(function(apps){
    var h = '<div class="modal-head"><h3>🚀 Kurulu Uygulamalar</h3><button class="mini-btn" onclick="closeModal()">✖</button></div><div class="app-list">';
    for (var i = 0; i < apps.length; i++) {
      h += '<div class="app-row"><div><div style="font-size:13px;font-weight:700">' + esc(apps[i].appName) + '</div><div style="font-size:11px;color:#94a3b8">' + esc(apps[i].packageName) + '</div></div>' +
        '<button class="mini-btn" onclick="launchApp(\'' + esc(apps[i].packageName) + '\')">Başlat</button></div>';
    }
    h += '</div>';
    openModal(h);
  }).catch(function(){ toast('Uygulamalar alınamadı!'); });
}

function launchApp(pkg) {
  fetch('/api/apps/launch?pkg=' + encodeURIComponent(pkg), { method: 'POST' }).then(function(){ toast('Uygulama başlatıldı!'); closeModal(); });
}

function openApkModal() {
  openModal(
    '<div class="modal-head"><h3>📦 Uzaktan APK Gönder</h3><button class="mini-btn" onclick="closeModal()">✖</button></div>' +
    '<div class="field"><label>APK Dosyası Seç (.apk)</label><input id="apkFile" type="file" accept=".apk" class="input"></div>' +
    '<div id="apkProgWrap" class="prog-wrap hidden"><div id="apkProgBar" class="prog-bar"></div></div>' +
    '<button id="btnDoApk" class="btn btn-teal btn-full" onclick="doUploadApk()">📦 Gönder ve Kur</button>'
  );
}

function doUploadApk() {
  var f = document.getElementById('apkFile').files[0];
  if (!f) { toast('Lütfen .apk dosyası seçin!'); return; }
  var btn = document.getElementById('btnDoApk');
  btn.disabled = true; btn.textContent = 'Gönderiliyor...';
  document.getElementById('apkProgWrap').classList.remove('hidden');
  var xhr = new XMLHttpRequest();
  xhr.open('POST', '/api/upload_raw?filename=remote.apk&apk=true', true);
  xhr.upload.onprogress = function(e) {
    if (e.lengthComputable) document.getElementById('apkProgBar').style.width = Math.round((e.loaded / e.total) * 100) + '%';
  };
  xhr.onload = function() { closeModal(); toast('APK gönderildi ve kurulum başlatıldı!'); };
  xhr.onerror = function() { btn.disabled = false; toast('APK gönderimi başarısız!'); };
  xhr.send(f);
}

function loadPeers() {
  fetch('/api/peers').then(function(r){ return r.json(); }).then(function(list){
    var box = document.getElementById('peersBox');
    if (!list || !list.length) {
      box.innerHTML = '<div style="color:#94a3b8;font-size:13px;padding:10px">Başka cihaz bulunamadı.</div>';
      return;
    }
    var h = '';
    for (var i = 0; i < list.length; i++) {
      var p = list[i];
      h += '<div class="peer-item"><div><div style="font-size:14px;font-weight:700"><span class="dot"></span> ' + esc(p.tag) + (p.isSelf ? ' (Şu an bağlı)' : '') + '</div>' +
        '<div style="font-size:11px;color:#94a3b8">' + esc(p.host) + ' • ID: ' + esc(p.id) + '</div></div>' +
        (p.isSelf ? '<span class="badge b-img">AKTİF</span>' : '<a class="btn btn-blue" style="text-decoration:none;padding:8px 12px" href="http://' + esc(p.host) + '/">Yönet →</a>') +
        '</div>';
    }
    box.innerHTML = h;
  }).catch(function(){});
}

function sendInput(q) { fetch('/api/input?' + q, { method: 'POST' }); }

document.getElementById('loginPass').addEventListener('keydown', function(e){ if (e.key === 'Enter') doLogin(); });

if (localStorage.getItem('mb_web_auth') === 'ok_152') showApp();
</script>
</body>
</html>
""".trimIndent()
}
