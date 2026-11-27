const currentDate = document.getElementById("current-date");
const collapseStorageKey = "sf.sidebar.collapsed";

// Estado inicial del menú lateral en escritorio para evitar saltos al navegar.
try {
	if (typeof window.localStorage?.getItem === "function" && window.localStorage.getItem(collapseStorageKey) === "1") {
		document.documentElement?.classList?.add("sidebar-collapsed");
	}
} catch (_) { /* Sin persistencia disponible. */ }

// Un solo comportamiento de navegación para todas las pantallas.
const appSidebar = document.getElementById("appSidebar");
if (appSidebar) {
	const closeSidebar = () => {
		if (window.bootstrap?.Offcanvas) window.bootstrap.Offcanvas.getOrCreateInstance(appSidebar).hide();
		else appSidebar.classList.remove("show");
	};
	appSidebar.querySelectorAll('[data-bs-dismiss="offcanvas"]').forEach(button => button.addEventListener("click", closeSidebar));
	appSidebar.querySelectorAll("nav a").forEach(link => link.addEventListener("click", () => {
		if (window.matchMedia("(max-width: 991.98px)").matches) closeSidebar();
	}));
	document.querySelectorAll('[data-bs-target="#appSidebar"][data-bs-toggle="offcanvas"]').forEach(button => {
		appSidebar.addEventListener("shown.bs.offcanvas", () => button.setAttribute("aria-expanded", "true"));
		appSidebar.addEventListener("hidden.bs.offcanvas", () => button.setAttribute("aria-expanded", "false"));
	});

	// Modo riel colapsable en escritorio.
	const collapseBtn = document.getElementById("sidebarCollapseBtn");
	const syncCollapseBtn = () => {
		if (!collapseBtn) return;
		const collapsed = Boolean(document.documentElement?.classList?.contains("sidebar-collapsed"));
		collapseBtn.setAttribute("aria-expanded", String(!collapsed));
		const label = collapsed ? "Expandir menú lateral" : "Contraer menú lateral";
		collapseBtn.setAttribute("aria-label", label);
		collapseBtn.title = label;
	};
	syncCollapseBtn();
	collapseBtn?.addEventListener("click", () => {
		const collapsed = document.documentElement?.classList?.toggle("sidebar-collapsed");
		syncCollapseBtn();
		try { window.localStorage?.setItem?.(collapseStorageKey, collapsed ? "1" : "0"); } catch (_) { /* Ignorar */ }
	});

	// Submenús tipo acordeón (Accesos y roles).
	appSidebar.querySelectorAll("[data-sidebar-group]").forEach(group => {
		const toggleBtn = group.querySelector?.(".sidebar-group-toggle");
		toggleBtn?.addEventListener("click", () => {
			if (document.documentElement?.classList?.contains("sidebar-collapsed") && window.matchMedia("(min-width: 992px)").matches) {
				document.documentElement.classList.remove("sidebar-collapsed");
				syncCollapseBtn();
				try { window.localStorage?.setItem?.(collapseStorageKey, "0"); } catch (_) { /* Ignorar */ }
				group.classList.add("is-open");
				toggleBtn.setAttribute("aria-expanded", "true");
				return;
			}
			const abierto = group.classList.toggle("is-open");
			toggleBtn.setAttribute("aria-expanded", String(abierto));
		});
	});
}

if (currentDate) {
	currentDate.textContent = new Intl.DateTimeFormat("es", {
		weekday: "long",
		day: "numeric",
		month: "long",
		year: "numeric"
	}).format(new Date());
}

// Búsqueda en vivo sin presionar Enter en todos los listados con filtro GET.
if (typeof window.fetch === "function" && typeof DOMParser === "function" && typeof FormData === "function") {
	for (const form of document.querySelectorAll('.cliente-panel form[method="get"]')) {
		if (typeof form?.querySelector !== "function") continue;
		const campoQ = form.querySelector('input[name="q"]');
		const panel = form.closest?.(".cliente-panel");
		if (!campoQ || !panel) continue;

		let temporizador = null;
		let controlador = null;

		const ejecutarBusqueda = async () => {
			if (controlador) controlador.abort();
			controlador = typeof AbortController === "function" ? new AbortController() : null;
			const action = form.getAttribute("action") || window.location.pathname;
			const url = new URL(action, window.location.origin);
			const params = new URLSearchParams();
			for (const [k, v] of new FormData(form).entries()) {
				if (k !== "pagina" && String(v).trim() !== "") params.set(k, String(v));
			}
			url.search = params.toString();

			try {
				const resp = await window.fetch(url.toString(), {
					signal: controlador?.signal,
					headers: { "X-Requested-With": "fetch" }
				});
				if (resp.redirected && resp.url.includes("/login")) {
					window.location.href = resp.url;
					return;
				}
				if (!resp.ok) return;
				const doc = new DOMParser().parseFromString(await resp.text(), "text/html");
				const nuevoPanel = doc.querySelector(".cliente-panel");
				if (!nuevoPanel) return;

				const tablaActual = panel.querySelector(".table-responsive");
				const tablaNueva = nuevoPanel.querySelector(".table-responsive");
				if (tablaActual && tablaNueva) tablaActual.replaceWith(tablaNueva);

				const navActual = panel.querySelector("nav");
				const navNueva = nuevoPanel.querySelector("nav");
				if (navActual && navNueva) navActual.replaceWith(navNueva);
				else if (navActual && !navNueva) navActual.remove();
				else if (!navActual && navNueva) panel.appendChild(navNueva);

				window.history?.replaceState?.(null, "", url.toString());
			} catch (err) {
				if (err?.name !== "AbortError") { /* Ignorar errores transitorios de red */ }
			}
		};

		const programar = () => {
			clearTimeout(temporizador);
			temporizador = setTimeout(ejecutarBusqueda, 260);
		};

		campoQ.addEventListener("input", programar);
		campoQ.addEventListener("search", ejecutarBusqueda);
		for (const filtroExtra of form.querySelectorAll('select, input[type="date"]')) {
			filtroExtra.addEventListener("change", ejecutarBusqueda);
		}
	}
}
