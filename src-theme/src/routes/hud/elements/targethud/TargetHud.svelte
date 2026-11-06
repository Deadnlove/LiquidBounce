<script lang="ts">
    import {onDestroy} from "svelte";
    import {menuEase} from "../../../../util/menuMotion";
    import ArmorStatus from "./ArmorStatus.svelte";
    import {listen} from "../../../../integration/ws.js";
    import type {PlayerData} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import {fly} from "svelte/transition";
    import HealthProgress from "./HealthProgress.svelte";
    import type {TargetChangeEvent} from "../../../../integration/events";

    let target: PlayerData | null = null;
    let visible = false;

    let hideTimeout: number;

    function startHideTimeout() {
        hideTimeout = setTimeout(() => {
            visible = false;
        }, 1000);
    }

    listen("targetChange", (data: TargetChangeEvent) => {
        clearTimeout(hideTimeout);
        if (data.target == null) {
            visible = false;
            return;
        }
        target = data.target;
        visible = true;
        startHideTimeout();
    });

    onDestroy(() => clearTimeout(hideTimeout));
</script>

{#if visible && target != null}
    <div class="targethud" transition:fly={{ y: -10, duration: 220, easing: menuEase }}>
        <div class="main-wrapper">
            <div class="avatar">
                <img src="{REST_BASE}/api/v1/client/resource/skin?uuid={target.uuid}" alt="avatar" />
            </div>
    
            <div class="name">{target.username}</div>
            <div class="health-stats">
                <div class="stat">
                    <div class="value">{Math.floor(target.actualHealth)}</div>
                    <img
                            class="icon"
                            src="img/hud/targethud/icon-health.svg"
                            alt="health"
                    />
                </div>
                {#if target.absorption > 0}
                    <div class="stat">
                        <div class="value">{Math.floor(target.absorption)}</div>
                        <img
                                class="icon"
                                src="img/hud/targethud/icon-absorption.svg"
                                alt="absorption"
                        />
                    </div>
                {/if}
                <div class="stat">
                    <div class="value">{Math.floor(target.armor)}</div>
                    <img
                            class="icon"
                            src="img/hud/targethud/icon-armor.svg"
                            alt="armor"
                    />
                </div>
            </div>
            <div class="armor-stats">
                {#if target.armorItems[3]?.count > 0}
                    <ArmorStatus itemStack={target.armorItems[3]} />
                {/if}
                {#if target.armorItems[2]?.count > 0}
                    <ArmorStatus itemStack={target.armorItems[2]} />
                {/if}
                {#if target.armorItems[1]?.count > 0}
                    <ArmorStatus itemStack={target.armorItems[1]} />
                {/if}
                {#if target.armorItems[0]?.count > 0}
                    <ArmorStatus itemStack={target.armorItems[0]} />
                {/if}
            </div>
        </div>    
        
        <HealthProgress maxHealth={target.maxHealth + target.absorption} health={target.actualHealth + target.absorption} />
    </div>
{/if}

<style lang="scss">

    .targethud {
        background: color-mix(in srgb, var(--targethud-background-color) 45%, var(--hud-surface));
        border: 1px solid var(--hud-border);
        min-width: 260px;
        border-radius: var(--hud-radius);
        overflow: hidden;
    }

    .main-wrapper {
        display: grid;
        grid-template-areas:
            "a b d"
            "a c d";
        column-gap: 12px;
        padding: 14px 16px;
    }

    .name {
        grid-area: b;
        color: var(--targethud-text-color);
        font-weight: 600;
        letter-spacing: -.3px;
        align-self: flex-end;
    }

    .health-stats {
        grid-area: c;
        display: flex;
        column-gap: 12px;

        .stat {
            .value {
                color: var(--targethud-text-dimmed-color);
                font-size: 14px;
                min-width: 18px;
                display: inline-block;
            }
        }
    }

    .armor-stats {
        grid-area: d;
        display: flex;
        align-items: center;
        column-gap: 12px;
        padding-left: 5px;
    }

    .avatar {
        grid-area: a;
        height: 48px;
        width: 48px;
        position: relative;
        image-rendering: pixelated;
        background-image: url("/img/steve.png");
        background-repeat: no-repeat;
        background-size: cover;
        border-radius: var(--hud-radius);
        overflow: hidden;

        img {
            position: absolute;
            scale: 6.25;
            left: 118px;
            top: 118px;
        }
    }
</style>
