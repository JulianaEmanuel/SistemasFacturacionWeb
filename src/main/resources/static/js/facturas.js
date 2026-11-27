(() => {
    "use strict";
    document.getElementById("factura-imprimir")?.addEventListener("click", () => window.print());
    const form = document.getElementById("factura-form");
    if (!form) return;
    const body = document.getElementById("factura-lineas");
    const aviso = document.getElementById("factura-aviso");
    // Enteros escalados: evita errores de coma flotante en la previsualización.
    function scaled(text, scale) {
        const match = /^(\d+)(?:\.(\d+))?$/.exec(text);
        if (!match || (match[2] || "").length > scale) throw new Error("Revisa las cantidades y los productos.");
        return BigInt(match[1]) * 10n ** BigInt(scale) + BigInt((match[2] || "").padEnd(scale, "0") || "0");
    }
    const rounded = (value, divisor) => (value + divisor / 2n) / divisor;
    function money(value) {
        const cents = rounded(value, 100n);
        return `${cents / 100n}.${String(cents % 100n).padStart(2, "0")}`;
    }
    function rows() { return [...body.querySelectorAll(".factura-linea")]; }
    function quantity(input) {
        const text = input.value.trim();
        if (!/^[0-9]{1,6}([.,][0-9]{1,2})?$/.test(text)) throw new Error("Cantidad: usa hasta 6 enteros y 2 decimales, con coma o punto y sin separadores de miles.");
        const value = scaled(text.replace(",", "."), 2);
        if (value <= 0n || value > 99999999n) throw new Error("La cantidad debe estar entre 0.01 y 999999.99.");
        return value;
    }
    function stepQuantity(input, direction) {
        let units;
        try {
            units = input.value.trim() === "" ? 0n : quantity(input);
        } catch (error) {
            aviso.textContent = error.message;
            return;
        }
        const next = units + direction * 100n;
        if (next < 1n || next > 99999999n) return;
        const separator = input.value.includes(",") ? "," : ".";
        const decimal = String(next % 100n).padStart(2, "0").replace(/0+$/, "");
        input.value = decimal ? (next / 100n) + separator + decimal : String(next / 100n);
        update();
    }
    function percentage(rate) {
        const decimal = String(rate % 100n).padStart(2, "0").replace(/0+$/, "");
        return `${rate / 100n}${decimal ? "," + decimal : ""} %`;
    }
    function reindex() {
        rows().forEach((row, i) => {
            row.querySelector("select").name = `lineas[${i}].idProducto`;
            row.querySelector("input").name = `lineas[${i}].cantidad`;
        });
    }
    function update() {
        reindex(); let subtotal = 0n, iva = 0n, total = 0n, base15 = 0n, base5 = 0n, base0 = 0n;
        aviso.textContent = "";
        try {
            rows().forEach(row => {
                const select = row.querySelector("select"), option = select.selectedOptions[0];
                if (!select.value) { row.querySelectorAll("[data-valor]").forEach(cell => cell.textContent = "—"); return; }
                const price = scaled(option.dataset.precio, 4), rate = scaled(option.dataset.iva, 2);
                row.querySelector('[data-valor="precio"]').textContent = money(price);
                row.querySelector('[data-valor="porcentaje"]').textContent = percentage(rate);
                const units = quantity(row.querySelector("input"));
                const base = rounded(price * units, 100n), tax = rounded(base * rate, 10000n), sum = base + tax;
                const values = { precio: money(price), porcentaje: percentage(rate), subtotal: money(base), iva: money(tax), total: money(sum) };
                Object.entries(values).forEach(([key, value]) => row.querySelector(`[data-valor="${key}"]`).textContent = value);
                subtotal += base; iva += tax; total += sum;
                if (rate === 1500n) base15 += base;
                else if (rate === 500n) base5 += base;
                else if (rate === 0n) base0 += base;
            });
            document.getElementById("factura-subtotal").textContent = money(subtotal);
            document.getElementById("factura-iva").textContent = money(iva);
            document.getElementById("factura-total").textContent = money(total);
            const b15 = document.getElementById("factura-base-15"), b5 = document.getElementById("factura-base-5"), b0 = document.getElementById("factura-base-0");
            if (b15) b15.textContent = money(base15);
            if (b5) b5.textContent = money(base5);
            if (b0) b0.textContent = money(base0);
            return true;
        } catch (error) {
            aviso.textContent = error.message;
            rows().forEach(row => ["subtotal", "iva", "total"].forEach(key => row.querySelector(`[data-valor="${key}"]`).textContent = "—"));
            ["factura-subtotal", "factura-iva", "factura-total", "factura-base-15", "factura-base-5", "factura-base-0"].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.textContent = "—";
            });
            return false;
        }
    }
    function add() {
        if (rows().length >= 200) { aviso.textContent = "La factura admite hasta 200 líneas."; return; }
        body.append(document.getElementById("factura-linea-template").content.cloneNode(true)); update();
    }
    function combine(changed) {
        const select = changed.querySelector("select");
        if (!select.value) return;
        const existing = rows().find(row => row !== changed && row.querySelector("select").value === select.value);
        if (!existing) return;
        try {
            const sum = quantity(existing.querySelector("input")) + quantity(changed.querySelector("input"));
            if (sum > 99999999n) throw new Error("La cantidad acumulada excede el máximo permitido.");
            existing.querySelector("input").value = `${sum / 100n}.${String(sum % 100n).padStart(2, "0")}`;
            changed.remove();
        } catch (error) { select.value = ""; aviso.textContent = error.message; }
    }
    document.getElementById("factura-agregar").addEventListener("click", add);
    body.addEventListener("input", update);
    body.addEventListener("change", event => { if (event.target.matches("select")) combine(event.target.closest("tr")); update(); });

    const modalProductos = document.getElementById("modalProductos");
    const buscarProducto = document.getElementById("modal-producto-buscar");
    const contadorProductos = document.getElementById("modal-producto-contador");
    const vacioProductos = document.getElementById("modal-productos-vacio");
    const filasProductos = [...(document.querySelectorAll?.(".modal-producto-fila") || [])].filter(f => f?.dataset?.productoId !== undefined);
    let filaObjetivo = null;

    document.getElementById("factura-agregar-modal")?.addEventListener("click", () => { filaObjetivo = null; });

    body.addEventListener("click", event => {
        const step = event.target.closest("[data-paso-cantidad]");
        if (step) {
            const input = step.closest("tr").querySelector(".factura-cantidad");
            stepQuantity(input, BigInt(step.dataset.pasoCantidad));
        } else if (event.target.closest(".btn-buscar-producto")) {
            filaObjetivo = event.target.closest("tr");
            if (modalProductos && typeof bootstrap !== "undefined" && bootstrap.Modal) {
                bootstrap.Modal.getOrCreateInstance(modalProductos).show();
            }
        } else if (event.target.closest(".factura-quitar")) {
            event.target.closest("tr").remove();
            update();
        }
    });

    if (buscarProducto) {
        buscarProducto.addEventListener("input", () => {
            const q = buscarProducto.value.trim().toLowerCase();
            let visibles = 0;
            for (const fila of filasProductos) {
                const coincide = !q || (fila.dataset?.busqueda || "").includes(q);
                fila.hidden = !coincide;
                if (coincide) visibles++;
            }
            if (vacioProductos) vacioProductos.hidden = visibles > 0;
            if (contadorProductos) contadorProductos.textContent = visibles === 1 ? "1 producto" : `${visibles} productos`;
        });
    }

    if (modalProductos) {
        modalProductos.addEventListener("click", event => {
            const btn = event.target?.closest?.(".seleccionar-producto");
            if (!btn) return;
            let destino = filaObjetivo && rows().includes(filaObjetivo) ? filaObjetivo : rows().find(r => !r.querySelector("select").value);
            if (!destino) { add(); destino = rows().at(-1); }
            if (destino) {
                destino.querySelector("select").value = btn.dataset.id || "";
                combine(destino);
                update();
            }
            filaObjetivo = null;
            if (typeof bootstrap !== "undefined" && bootstrap.Modal) bootstrap.Modal.getOrCreateInstance(modalProductos).hide();
            else modalProductos.querySelector?.('[data-bs-dismiss="modal"]')?.click?.();
        });
        modalProductos.addEventListener("shown.bs.modal", () => {
            if (buscarProducto) { buscarProducto.focus(); buscarProducto.select?.(); }
        });
    }
    form.addEventListener("submit", event => {
        if (!update()) { event.preventDefault(); return; }
        if (!form.reportValidity() || !rows().length) { event.preventDefault(); aviso.textContent = "Selecciona un cliente y al menos un producto con cantidad válida."; return; }
        document.getElementById("factura-registrar").disabled = true;
    });
    window.addEventListener("pageshow", () => { if (!document.querySelector(".alert-warning")) document.getElementById("factura-registrar").disabled = false; });
    if (typeof document.addEventListener === "function") {
        document.addEventListener("keydown", event => {
            if (event.key === "F2") {
                const m = document.getElementById("modalClientes");
                if (m && typeof bootstrap !== "undefined" && bootstrap.Modal) { event.preventDefault(); bootstrap.Modal.getOrCreateInstance(m).show(); }
            } else if (event.key === "F4") {
                if (modalProductos && typeof bootstrap !== "undefined" && bootstrap.Modal) { event.preventDefault(); filaObjetivo = null; bootstrap.Modal.getOrCreateInstance(modalProductos).show(); }
            }
        });
    }
    if (!rows().length) add(); else { rows().forEach(combine); update(); }
})();
