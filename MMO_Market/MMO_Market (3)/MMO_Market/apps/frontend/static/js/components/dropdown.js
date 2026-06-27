(function initDsDropdowns() {
    window.initDsDropdowns = initAll;
    document.addEventListener('DOMContentLoaded', initAll);

    function initAll() {
        document.querySelectorAll('.ds-dropdown-panel').forEach(panel => {
            if (panel._dsAnchor && !panel._dsAnchor.isConnected) {
                panel.remove();
            }
        });

        document.querySelectorAll('.ds-dropdown').forEach(root => {
            if (root.dataset.dsDropdownReady === 'true') return;

            const toggle = root.querySelector('.ds-dropdown-toggle');
            const panel = root.querySelector('.ds-dropdown-panel');
            if (!toggle || !panel) return;

            root.dataset.dsDropdownReady = 'true';
            panel.hidden = true;
            panel.style.zIndex = '10010';
            panel._dsAnchor = root;
            document.body.appendChild(panel);

            toggle.setAttribute('aria-haspopup', 'listbox');
            toggle.setAttribute('aria-expanded', 'false');

            toggle.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const shouldOpen = panel.hidden;
                closeAll(panel);
                panel.hidden = !shouldOpen;
                toggle.setAttribute('aria-expanded', String(shouldOpen));
                if (shouldOpen) positionPanel(panel, root);
            });

            panel.addEventListener('click', event => {
                event.stopPropagation();
                if (event.target.closest('.ds-dropdown-option')) {
                    panel.hidden = true;
                    toggle.setAttribute('aria-expanded', 'false');
                }
            });
        });
    }

    function positionPanel(panel, root) {
        if (!panel || panel.hidden || !root?.isConnected) return;

        const margin = 8;
        const gap = 4;
        const anchor = root.getBoundingClientRect();
        const width = Math.min(Math.max(anchor.width, 220), window.innerWidth - margin * 2);
        panel.style.width = `${width}px`;

        const panelHeight = panel.offsetHeight;
        const spaceBelow = window.innerHeight - anchor.bottom - margin;
        const spaceAbove = anchor.top - margin;
        const openAbove = spaceBelow < panelHeight + gap && spaceAbove > spaceBelow;
        const top = openAbove
            ? Math.max(margin, anchor.top - panelHeight - gap)
            : Math.min(window.innerHeight - panelHeight - margin, anchor.bottom + gap);
        const left = Math.min(
            Math.max(margin, anchor.left),
            Math.max(margin, window.innerWidth - width - margin)
        );

        panel.style.top = `${Math.max(margin, top)}px`;
        panel.style.left = `${left}px`;
    }

    function closeAll(except) {
        document.querySelectorAll('.ds-dropdown-panel').forEach(panel => {
            if (panel === except) return;
            panel.hidden = true;
            panel._dsAnchor?.querySelector('.ds-dropdown-toggle')
                ?.setAttribute('aria-expanded', 'false');
        });
    }

    function repositionOpenPanels() {
        document.querySelectorAll('.ds-dropdown-panel:not([hidden])').forEach(panel => {
            positionPanel(panel, panel._dsAnchor);
        });
    }

    document.addEventListener('click', closeAll);
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') closeAll();
    });
    window.addEventListener('resize', repositionOpenPanels);
    window.addEventListener('scroll', repositionOpenPanels, true);
})();
