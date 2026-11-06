<script lang="ts">
    import {onMount} from "svelte";

    import {menuDuration} from "../../../util/menuMotion";

    const TRANSITION_DURATION = menuDuration;

    let ready = false;

    onMount(() => {
        const timeout = window.setTimeout(() => {
            ready = true;
        }, TRANSITION_DURATION);

        return () => window.clearTimeout(timeout);
    });
</script>

{#if ready}
    <slot/>
{/if}
