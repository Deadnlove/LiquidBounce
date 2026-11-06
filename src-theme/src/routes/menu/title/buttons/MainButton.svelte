<script lang="ts">
    import {fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import TitleButtonIcon from "./TitleButtonIcon.svelte";
    import {menuDuration, menuEase} from "../../../../util/menuMotion";

    export let title: string;
    export let description = "";
    export let icon: string;
    export let index: number;
    let hovered = false;
    const dispatch = createEventDispatcher();
</script>

<div class="main-button" role="group" aria-label={title} on:mouseenter={() => hovered = true} on:mouseleave={() => hovered = false}
     on:focusin={() => hovered = true} on:focusout={() => hovered = false}
     out:fly|global={{duration: menuDuration, x: -16, easing: menuEase}}
     in:fly|global={{duration: menuDuration, x: -16, delay: index * 35, easing: menuEase}}>
    <button class="primary" on:click={() => dispatch("click")}>
        <span class="ordinal" aria-hidden="true">{String(index + 1).padStart(2, "0")}</span>
        <span class="icon"><TitleButtonIcon {icon}/></span>
        <span class="label"><span class="title">{title}</span><span class="description">{description}</span></span>
        <span class="arrow" aria-hidden="true">↗</span>
    </button>
    <div class="wrapped-content"><slot parentHovered={hovered}/></div>
</div>

<style lang="scss">
    .main-button {
      position: relative;
      width: 100%;
      border-radius: var(--menu-radius, 18px);
      border: 1px solid rgba(255, 255, 255, .09);
      background: rgba(255, 255, 255, .045);
      transition: background .2s var(--menu-ease), border-color .2s var(--menu-ease), transform .2s var(--menu-ease);
      &:hover, &:focus-within {
        background: color-mix(in srgb, var(--accent-color) 10%, rgba(18, 25, 31, .85));
        border-color: color-mix(in srgb, var(--accent-color) 60%, transparent);
        transform: translateX(4px);
      }
    }
    .primary {
      width: 100%;
      border: none;
      border-radius: inherit;
      background: transparent;
      color: var(--menu-text-color);
      display: grid;
      grid-template-columns: 22px 58px 1fr 22px;
      text-align: left;
      align-items: center;
      gap: 22px;
      padding: 24px;
      cursor: pointer;
      font: inherit;
    }
    .ordinal { font-size: 11px; color: var(--accent-color); opacity: .55; font-weight: 700; letter-spacing: 1px; }
    .icon {
      color: var(--accent-color);
      background: color-mix(in srgb, var(--accent-color) 11%, transparent);
      border-radius: 16px;
      width: 58px;
      height: 58px;
      display: grid;
      place-items: center;
    }
    .label { display: flex; flex-direction: column; gap: 7px; }
    .title { font-size: 24px; font-weight: 600; letter-spacing: -.5px; }
    .description { font-size: 14px; opacity: .56; }
    .arrow { font-size: 23px; opacity: .45; }
    .wrapped-content { position: absolute; right: 60px; top: 50%; transform: translateY(-50%); }
</style>
