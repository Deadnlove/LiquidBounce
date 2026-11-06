<script lang="ts">
    import ToolTip from "../ToolTip.svelte";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;

    const dispatch = createEventDispatcher();
</script>

<button class="icon-button" aria-label={title} on:click={() => dispatch("click")}>
    <ToolTip text={title}/>

    <div class="icon">
        <img src="img/menu/icon-{icon}.svg" alt={icon}>
    </div>
</button>

<style lang="scss">
  .icon-button {
    position: relative;
    border: none;
    background: transparent;
    border-radius: 12px;
    transition: background .2s var(--menu-ease);
    &:hover { background: rgba(255, 255, 255, .08); }
  }

  .icon {
    height: 58px;
    width: 58px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }
</style>