<script lang="ts">
    import {onMount} from "svelte";
    import {getClientInfo} from "../../../integration/rest";
    let version = "";
    onMount(() => {
        let mounted = true;
        getClientInfo().then(info => { if (mounted) version = info.clientVersion; }).catch(() => {});
        return () => { mounted = false; };
    });
</script>

<div class="watermark" aria-label="Grateful Dead client">
    <span class="monogram" aria-hidden="true">GD</span>
    <div class="identity"><strong>Grateful Dead</strong><span>CLIENT{version ? ` / ${version}` : ""}</span></div>
</div>

<style>
    .watermark { display: flex; align-items: center; gap: 12px; width: max-content;
        padding: 10px 16px 10px 10px; border-radius: var(--hud-radius); background: var(--hud-surface);
        border: 1px solid var(--hud-border); color: var(--text-color); }
    .monogram { display: grid; place-items: center; width: 36px; height: 36px; border-radius: var(--hud-radius-small);
        color: #10171c; background: var(--accent-color); font-size: 14px; font-weight: 800; letter-spacing: -1px; }
    .identity { display: flex; flex-direction: column; gap: 4px; }
    strong { font-size: 14px; font-weight: 650; letter-spacing: -.3px; }
    .identity span { opacity: .5; font-size: 9px; letter-spacing: 1.5px; }
</style>
