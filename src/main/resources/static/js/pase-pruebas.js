const pase = window.location.hash.slice(1);
window.history.replaceState(null, '', window.location.pathname);
if (pase) {
    document.getElementById('pase').value = pase;
    document.getElementById('form-pase').requestSubmit();
} else {
    document.getElementById('estado-pase').textContent = 'Vuelve al sistema principal y usa Entrar a pruebas.';
}
