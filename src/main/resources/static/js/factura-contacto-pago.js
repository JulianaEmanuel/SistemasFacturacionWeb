(() => {
    const cliente=document.getElementById('idCliente');
    if(!cliente)return;
    const nombreEl=document.getElementById('cliente-contacto-nombre');
    const cedulaEl=document.getElementById('cliente-contacto-cedula');
    const modalEl=document.getElementById('modalClientes');
    const buscarInput=document.getElementById('modal-cliente-buscar');
    const contadorEl=document.getElementById('modal-cliente-contador');
    const vacioRow=document.getElementById('modal-clientes-vacio');
    const prevBtn=document.getElementById('modal-cliente-prev');
    const nextBtn=document.getElementById('modal-cliente-next');
    const paginaEl=document.getElementById('modal-cliente-pagina');
    const filasModal=[...(document.querySelectorAll?.('.modal-cliente-fila')||[])].filter(f=>f?.dataset?.clienteId!==undefined);
    const POR_PAGINA=5;
    let paginaModal=0;

    const marcarFilaSeleccionada=()=>{
        const idActual=cliente.value||'';
        for(const fila of filasModal) fila.classList?.toggle('is-selected',idActual!=='' && fila.dataset?.clienteId===idActual);
    };

    const renderizarModalClientes=()=>{
        if(!filasModal.length && !vacioRow) return;
        const q=(buscarInput?.value||'').trim().toLowerCase();
        const filtradas=filasModal.filter(fila=>!q || (fila.dataset?.busqueda||'').includes(q));
        const total=filtradas.length;
        const totalPaginas=Math.max(1,Math.ceil(total/POR_PAGINA));
        if(paginaModal>=totalPaginas) paginaModal=totalPaginas-1;
        if(paginaModal<0) paginaModal=0;
        const inicio=paginaModal*POR_PAGINA;
        const fin=inicio+POR_PAGINA;
        const visiblesSet=new Set(filtradas.slice(inicio,fin));
        for(const fila of filasModal) fila.hidden=!visiblesSet.has(fila);
        if(vacioRow) vacioRow.hidden=total>0;
        if(contadorEl) contadorEl.textContent=total===1?'1 cliente':`${total} clientes`;
        if(paginaEl) paginaEl.textContent=`Página ${paginaModal+1} de ${totalPaginas}`;
        if(prevBtn) prevBtn.disabled=paginaModal<=0;
        if(nextBtn) nextBtn.disabled=paginaModal+1>=totalPaginas;
    };

    const pintar=()=>{
        const option=cliente.selectedOptions?.[0];
        if(nombreEl) nombreEl.textContent=option?.dataset?.nombre || (cliente.value ? option?.textContent : 'Ningún cliente seleccionado');
        if(cedulaEl) cedulaEl.textContent=option?.dataset?.cedula || 'Sin cédula';
        for(const [campo,titulo] of [['direccion','Dirección'],['correo','Correo'],['telefono','Teléfono']]) {
            const destino=document.getElementById(`cliente-contacto-${campo}`);
            if(destino) destino.textContent=`${titulo}: ${option?.dataset?.[campo] || 'No registrado'}`;
        }
        marcarFilaSeleccionada();
    };
    cliente.addEventListener('change',pintar);pintar();renderizarModalClientes();

    if(buscarInput) {
        buscarInput.addEventListener('input',()=>{ paginaModal=0; renderizarModalClientes(); });
    }
    if(prevBtn) prevBtn.addEventListener('click',()=>{ if(paginaModal>0){ paginaModal--; renderizarModalClientes(); } });
    if(nextBtn) nextBtn.addEventListener('click',()=>{ paginaModal++; renderizarModalClientes(); });

    if(modalEl) {
        modalEl.addEventListener('click',event=>{
            const btn=event.target?.closest?.('.seleccionar-cliente');
            if(!btn) return;
            cliente.value=btn.dataset.id||'';
            if(typeof Event==='function' && typeof cliente.dispatchEvent==='function') cliente.dispatchEvent(new Event('change'));
            else pintar();
            if(typeof bootstrap!=='undefined' && bootstrap.Modal) bootstrap.Modal.getOrCreateInstance(modalEl).hide();
            else modalEl.querySelector?.('[data-bs-dismiss="modal"]')?.click?.();
        });
        modalEl.addEventListener('shown.bs.modal',()=>{
            if(buscarInput) { buscarInput.focus(); buscarInput.select?.(); }
        });
    }

    // Centavos enteros: evita errores de coma flotante. null si el texto no es un monto válido.
    const centavos=texto=>{
        const m=/^(\d{1,8})(?:[.,](\d{1,2}))?$/.exec((texto||'').trim());
        return m?Number(m[1])*100+Number((m[2]||'').padEnd(2,'0')||0):null;
    };
    const dinero=c=>`$ ${Math.floor(c/100)}.${String(c%100).padStart(2,'0')}`;

    const total=document.getElementById('factura-total'),pago=document.getElementById('factura-pago-importe');
    const forma=document.getElementById('idFormaPago'),recibido=document.getElementById('montoRecibido'),cambio=document.getElementById('factura-pago-cambio');
    const grupos=[...(document.querySelectorAll?.('[data-pago-grupo]')||[])];

    const calcularCambio=()=>{
        if(!cambio)return;
        const aPagar=centavos(total?.textContent),dado=centavos(recibido?.value);
        if(aPagar===null){cambio.textContent='—';return;}
        if(!recibido?.value?.trim()){cambio.textContent=dinero(0);return;}
        if(dado===null){cambio.textContent='Monto no válido';return;}
        cambio.textContent=dado<aPagar?`Faltan ${dinero(aPagar-dado)}`:dinero(dado-aPagar);
    };
    // Solo se muestran y envían los campos de la forma de pago elegida.
    const mostrarGrupos=()=>{
        const codigo=forma?.selectedOptions?.[0]?.dataset?.codigo||'';
        for(const g of grupos) g.hidden=!g.dataset.pagoGrupo.split(' ').includes(codigo);
        for(const g of grupos) for(const campo of g.querySelectorAll('input,select'))
            campo.disabled=campo.closest('[data-pago-grupo][hidden]')!==null;
    };
    if(forma){forma.addEventListener('change',mostrarGrupos);mostrarGrupos();}
    if(recibido) recibido.addEventListener('input',calcularCambio);

    if(total&&pago){const actualizar=()=>{pago.textContent=`$ ${total.textContent}`;calcularCambio();};new MutationObserver(actualizar).observe(total,{childList:true,characterData:true,subtree:true});actualizar();}
})();
