<script lang="ts">
    import Header from "./header/Header.svelte";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";

    import {menuDuration, menuEase} from "../../../util/menuMotion";

    const transitionDuration = menuDuration;

    let ready = false;

    onMount(() => {
        const timer = window.setTimeout(() => {
            ready = true;
        }, transitionDuration);
        return () => window.clearTimeout(timer);
    });
</script>

<div class="menu">
    {#if ready}
        <div transition:fly|global={{duration: menuDuration, y: -16, easing: menuEase}}>
            <Header/>
        </div>
    {/if}

    <div class="menu-wrapper">
        <slot/>
    </div>
</div>

<style lang="scss">
  .menu {
    --menu-radius: 18px;
    --menu-ease: cubic-bezier(.333333, 0, .666667, 1);
    padding: clamp(28px, 3.4vw, 100px);
    color: var(--menu-text-color);
    background: radial-gradient(ellipse at 80% 25%, color-mix(in srgb, var(--accent-color) 9%, transparent), transparent 55%),
      linear-gradient(110deg, rgba(10, 15, 21, .94), rgba(10, 15, 21, .72) 65%, rgba(10, 15, 21, .86));
    display: flex;
    flex-direction: column;
    // Not sized in vh, which not every browser scales with zoom
    position: fixed;
    inset: 0;
    overflow: auto;

    :global(button:focus-visible), :global([role="button"]:focus-visible) {
      outline: 2px solid var(--accent-color);
      outline-offset: 5px;
    }

    @media (prefers-reduced-motion: reduce) {
      :global(*), :global(*::before), :global(*::after) {
        animation-duration: .01ms !important;
        transition-duration: .01ms !important;
      }
    }
  }

  .menu-wrapper {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    will-change: transform;
  }

  @media screen and (max-width: 1366px) {
    .menu {
      zoom: 0.8;
    }
  }

  @media screen and (max-width: 1200px) {
    .menu {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 1100px) {
    .menu {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .menu {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 540px) {
    .menu {
      zoom: 0.4;
    }
  }
</style>
