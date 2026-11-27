const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const code=fs.readFileSync(path.resolve(__dirname,'../../main/resources/static/js/app.js'),'utf8');
function fixture(mobile){let closed=0;const events={},attrs={};const close={addEventListener(_,fn){this.click=fn;}},link={addEventListener(_,fn){this.click=fn;}},toggle={setAttribute(k,v){attrs[k]=v;}};
 const sidebar={querySelectorAll(q){return q==='nav a'?[link]:[close];},addEventListener(e,fn){events[e]=fn;}};
 vm.runInNewContext(code,{document:{getElementById(id){return id==='appSidebar'?sidebar:null;},querySelectorAll(){return[toggle];}},window:{matchMedia(){return{matches:mobile};},bootstrap:{Offcanvas:{getOrCreateInstance(){return{hide(){closed++;}};}}}}});return{close,link,events,attrs,closed:()=>closed};}
test('móvil cierra al seleccionar opción y al pulsar X',()=>{const f=fixture(true);f.link.click();assert.equal(f.closed(),1);f.close.click();assert.equal(f.closed(),2);});
test('escritorio no cierra por seleccionar una opción',()=>{const f=fixture(false);f.link.click();assert.equal(f.closed(),0);});
test('el estado accesible sigue al offcanvas',()=>{const f=fixture(true);f.events['shown.bs.offcanvas']();assert.equal(f.attrs['aria-expanded'],'true');f.events['hidden.bs.offcanvas']();assert.equal(f.attrs['aria-expanded'],'false');});
test('búsqueda en vivo actualiza tabla y paginación al escribir sin presionar Enter',async()=>{
 const listeners={},fetched=[];let replacedUrl=null,tablaReplacedWith=null,navReplacedWith=null;
 const tablaActual={id:'old-table',replaceWith(n){tablaReplacedWith=n;}},navActual={id:'old-nav',replaceWith(n){navReplacedWith=n;}};
 const tablaNueva={id:'new-table'},navNueva={id:'new-nav'};
 const input={value:'Carlos',addEventListener(e,fn){listeners[e]=fn;}};
 const panel={querySelector(s){return s==='.table-responsive'?tablaActual:s==='nav'?navActual:null;},appendChild(){}};
 const form={getAttribute(){return '/modulos/clientes';},closest(s){return s==='.cliente-panel'?panel:null;},querySelector(s){return s==='input[name="q"]'?input:null;},querySelectorAll(){return[];}};
 const nuevoPanel={querySelector(s){return s==='.table-responsive'?tablaNueva:s==='nav'?navNueva:null;}};
 class MockDOMParser{parseFromString(){return{querySelector(s){return s==='.cliente-panel'?nuevoPanel:null;}};}}
 class MockFormData{entries(){return[['q',input.value],['pagina','2'],['vacio','  ']];}}
 const timers=[];
 const sandbox={
  URL,URLSearchParams,DOMParser:MockDOMParser,FormData:MockFormData,AbortController,
  setTimeout(fn,ms){const t={fn,ms,cleared:false};timers.push(t);return t;},
  clearTimeout(t){if(t)t.cleared=true;},
  document:{getElementById(){return null;},querySelectorAll(s){return s.includes('form[method="get"]')?[form]:[];}},
  window:{
   matchMedia(){return{matches:false};},
   location:{origin:'http://localhost',pathname:'/modulos/clientes'},
   history:{replaceState(_s,_t,u){replacedUrl=u;}},
   async fetch(url){fetched.push(url);return{ok:true,redirected:false,url,async text(){return '<html></html>';}};}
  }
 };
 vm.runInNewContext(code,sandbox);
 assert.equal(typeof listeners.input,'function');
 listeners.input();
 input.value='Ana';
 listeners.input();
 assert.equal(timers[0].cleared,true);
 assert.equal(timers[1].ms,260);
 await timers[1].fn();
 assert.deepEqual(fetched,['http://localhost/modulos/clientes?q=Ana']);
 assert.equal(tablaReplacedWith,tablaNueva);
 assert.equal(navReplacedWith,navNueva);
 assert.equal(replacedUrl,'http://localhost/modulos/clientes?q=Ana');
});
test('colapsa el menú lateral en escritorio y alterna acordeón de seguridad',()=>{
 const classes=new Set(),stored={},collapseAttrs={},groupClasses=new Set(),groupAttrs={};
 const classList={add(c){classes.add(c);},remove(c){classes.delete(c);},toggle(c){if(classes.has(c)){classes.delete(c);return false;}classes.add(c);return true;},contains(c){return classes.has(c);}};
 const collapseBtn={setAttribute(k,v){collapseAttrs[k]=v;},addEventListener(e,fn){this[e]=fn;}};
 const toggleBtn={setAttribute(k,v){groupAttrs[k]=v;},addEventListener(e,fn){this[e]=fn;}};
 const group={classList:{toggle(c){if(groupClasses.has(c)){groupClasses.delete(c);return false;}groupClasses.add(c);return true;},add(c){groupClasses.add(c);}},querySelector(s){return s==='.sidebar-group-toggle'?toggleBtn:null;}};
 const sidebar={querySelectorAll(q){return q==='[data-sidebar-group]'?[group]:[];},addEventListener(){}};
 vm.runInNewContext(code,{
  document:{documentElement:{classList},getElementById(id){return id==='appSidebar'?sidebar:id==='sidebarCollapseBtn'?collapseBtn:null;},querySelectorAll(){return[];}},
  window:{matchMedia(){return{matches:true};},localStorage:{getItem(k){return stored[k]||null;},setItem(k,v){stored[k]=v;}}}
 });
 collapseBtn.click();
 assert.equal(classes.has('sidebar-collapsed'),true);
 assert.equal(collapseAttrs['aria-expanded'],'false');
 assert.equal(stored['sf.sidebar.collapsed'],'1');
 toggleBtn.click();
 assert.equal(classes.has('sidebar-collapsed'),false);
 assert.equal(groupClasses.has('is-open'),true);
 assert.equal(groupAttrs['aria-expanded'],'true');
});
