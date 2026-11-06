<script lang="ts">
    export let maxHealth: number;
    export let health: number;

    $: safeMax = Number.isFinite(maxHealth) && maxHealth > 0 ? maxHealth : 1;
    $: safeHealth = Number.isFinite(health) ? Math.min(safeMax, Math.max(0, health)) : 0;
    $: width = safeHealth / safeMax * 100;
</script>

<div class="health-progress" role="progressbar" aria-label="Target health" aria-valuemin={0} aria-valuemax={safeMax} aria-valuenow={safeHealth}>
    <div class="thumb" style="width: {width}%;"></div>
</div>

<style lang="scss">

    .health-progress {
        position: relative;
        margin: 0 16px 14px;
        border-radius: 999px;
        overflow: hidden;
        background: rgba(255, 255, 255, .08);
    }

    .thumb {
        height: 5px;
        background-color: var(--targethud-health-progress-color);
        border-radius: inherit;
        transition: width .22s var(--hud-ease);
    }
</style>