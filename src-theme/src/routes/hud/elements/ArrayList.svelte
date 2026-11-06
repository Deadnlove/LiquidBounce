<script lang="ts">
    import {onDestroy, tick} from "svelte";
    import type {Module} from "../../../integration/types";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {getTextWidth} from "../../../integration/text_measurement";
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import type {HudArrayListSettings} from "../components";

    export let settings: { [name: string]: any };

    let cSettings = settings as HudArrayListSettings;

    let enabledModules: Module[] = [];
    let requestId = 0;
    let destroyed = false;
    import {menuEase} from "../../../util/menuMotion";

    async function updateEnabledModules() {
        const currentRequest = ++requestId;
        const modules = await getModules().catch(() => null);
        if (destroyed || currentRequest !== requestId || modules === null) return;
        const visibleModules = modules.filter(m => m.enabled && !m.hidden);

        const modulesWithWidths = visibleModules.map(module => {
            const formattedName = $spaceSeperatedNames ? convertToSpacedString(module.name) : module.name;
            const fullName = module.tag == null || !cSettings.showTags
                ? formattedName
                : formattedName + " " + module.tag;

            return {
                ...module,
                width: getTextWidth(fullName, "500 14px Inter")
            };
        });

        modulesWithWidths.sort((a, b) => cSettings.order === "Ascending" ? a.width - b.width : b.width - a.width);

        enabledModules = modulesWithWidths;
        await tick();
    }

    $: if (cSettings !== settings) {
        cSettings = settings as HudArrayListSettings;
        updateEnabledModules();
    }

    const unsubscribe = spaceSeperatedNames.subscribe(() => { updateEnabledModules(); });
    onDestroy(() => { destroyed = true; unsubscribe(); });

    listen("moduleToggle", async () => {
        await updateEnabledModules();
    });

    listen("refreshArrayList", async () => {
        await updateEnabledModules();
    });
</script>

<div class="arraylist" class:left={cSettings.itemAlignment === "Left"}>
    {#each enabledModules as {name, tag} (name)}
        <div
                class="module"
                style={cSettings.itemAlignment === "Left" ? "margin-right: auto;" : "margin-left: auto;"}
                animate:flip={{ duration: 220, easing: menuEase }}
                transition:fly={{ x: cSettings.itemAlignment === "Left" ? -12 : 12, duration: 220, easing: menuEase }}
        >
            {$spaceSeperatedNames ? convertToSpacedString(name) : name}
            {#if tag && cSettings.showTags}
                <span class="tag"> {tag}</span>
            {/if}
        </div>
    {/each}
</div>

<style lang="scss">

  .arraylist { display: flex; flex-direction: column; gap: 4px; }
  .left .module { border-right: none; border-left: solid 3px var(--arraylist-border-color); }
  .module {
    background: color-mix(in srgb, var(--arraylist-background-color) 50%, var(--hud-surface));
    color: var(--arraylist-text-color);
    font-size: 14px;
    border-radius: var(--hud-radius-small);
    padding: 7px 10px;
    border-right: solid 3px var(--arraylist-border-color);
    width: max-content;
    font-weight: 500;
    line-height: 1.25;
    border-top: 1px solid var(--hud-border);
  }

  .tag {
    color: var(--arraylist-tag-color);
  }
</style>
