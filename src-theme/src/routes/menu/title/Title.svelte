<script lang="ts">
    import MainButton from "./buttons/MainButton.svelte";
    import ChildButton from "./buttons/ChildButton.svelte";
    import ConfettiBackground from "./ConfettiBackground.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {
        exitClient,
        getClientUpdate,
        getClientInfo,
        openScreen,
        toggleBackgroundShaderEnabled,
        toggleBasicMode
    } from "../../../integration/rest";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";
    import {notification} from "../common/header/notification_store";
    import {menuDuration, menuEase} from "../../../util/menuMotion";
    import type {ClientInfo} from "../../../integration/types";
    import {isAnniversary} from "../../../util/utils";

    let regularButtonsShown = true;
    let clientButtonsShown = false;
    let clientInfo: ClientInfo | null = null;
    let transitionTimer: ReturnType<typeof setTimeout>;
    let changing = false;

    onMount(() => {
        getClientInfo().then(info => clientInfo = info).catch(() => {});
        const updateTimer = setTimeout(async () => {
            const clientUpdate = await getClientUpdate().catch(() => null);
            if (!clientUpdate) return;

            if (clientUpdate.update) {
                notification.set({
                    title: `Client update ${clientUpdate.update.clientVersion} is available`,
                    message: `Visit the website for release details.`,
                    error: false,
                    delay: 99999999
                });
            }
        }, 2000);
        return () => { clearTimeout(updateTimer); clearTimeout(transitionTimer); };
    });

    function toggleButtons() {
        if (changing) return;
        changing = true;
        const showClient = !clientButtonsShown;
        regularButtonsShown = false;
        clientButtonsShown = false;
        transitionTimer = setTimeout(() => {
            clientButtonsShown = showClient;
            regularButtonsShown = !showClient;
            changing = false;
        }, menuDuration);
    }

</script>

<div class="title-screen">
    {#if isAnniversary()}
        <ConfettiBackground/>
    {/if}

    <div class="content">
        <div class="navigation">
            <div class="section-label">{clientButtonsShown ? "YOUR CLIENT" : "START EXPLORING"}</div>
            <div class="main-buttons">
            {#if regularButtonsShown}
                <MainButton title="Singleplayer" description="Your worlds, at your pace" icon="singleplayer" index={0}
                            on:click={() => openScreen("singleplayer")}/>

                <MainButton title="Multiplayer" description="Find your next server" icon="multiplayer" let:parentHovered
                            on:click={() => openScreen("multiplayer")} index={1}>
                    <ChildButton title="Realms" icon="realms" {parentHovered}
                                 on:click={() => openScreen("multiplayer_realms")}/>
                </MainButton>
                <MainButton title="Grateful Dead" description="Modules, visuals & tools" icon="gratefuldead" on:click={toggleButtons} index={2}/>
                <MainButton title="Options" description="Make it feel like home" icon="options" on:click={() => openScreen("options")} index={3}/>
            {:else if clientButtonsShown}
                <MainButton title="Proxy Manager" description="Manage your connections" icon="proxymanager" on:click={() => openScreen("proxymanager")}
                            index={0}/>
                <MainButton title="Click GUI" description="Configure your modules" icon="clickgui" on:click={() => openScreen("clickgui")} index={1}/>
                <!-- <MainButton title="Scripts" icon="scripts" index={2}/> -->
                <MainButton title="Back" description="Return to the main menu" icon="back-large" on:click={toggleButtons} index={2}/>
            {/if}
            </div>
        </div>

        <aside class="identity" aria-label="Client information">
            <div class="cartography" aria-hidden="true">
                <svg viewBox="0 0 720 720" fill="none">
                    <defs>
                        <radialGradient id="map-glow"><stop stop-color="var(--accent-color)" stop-opacity=".2"/><stop offset="1" stop-color="var(--accent-color)" stop-opacity="0"/></radialGradient>
                        <linearGradient id="map-line" x1="90" y1="110" x2="630" y2="650" gradientUnits="userSpaceOnUse"><stop stop-color="var(--accent-color)" stop-opacity=".05"/><stop offset=".5" stop-color="var(--accent-color)" stop-opacity=".72"/><stop offset="1" stop-color="var(--accent-color)" stop-opacity=".05"/></linearGradient>
                    </defs>
                    <circle cx="360" cy="360" r="325" fill="url(#map-glow)"/>
                    <g stroke="url(#map-line)" stroke-width="1.4">
                        <path d="M38 378C87 158 207 54 380 60S684 195 679 357 541 654 370 660 13 550 38 378Z"/>
                        <path d="M78 385C119 205 215 101 380 104S644 212 638 359 525 606 371 613 58 530 78 385Z"/>
                        <path d="M120 392C151 246 238 149 377 149S600 233 597 357 506 560 369 566 104 506 120 392Z"/>
                        <path d="M166 395C187 275 259 197 378 194S555 252 555 360 485 515 368 519 153 485 166 395Z"/>
                        <circle class="orbit-ring" cx="360" cy="360" r="278" stroke-dasharray="5 12"/>
                        <circle cx="360" cy="360" r="128" stroke-opacity=".7"/>
                        <path d="M360 204V516M204 360H516" stroke-opacity=".3"/>
                    </g>
                    <g fill="var(--accent-color)"><circle cx="360" cy="232" r="5"/><circle cx="598" cy="266" r="4"/><circle cx="121" cy="393" r="3"/></g>
                    <circle cx="360" cy="360" r="42" fill="var(--accent-color)" fill-opacity=".09" stroke="var(--accent-color)" stroke-opacity=".6"/>
                    <text x="360" y="369" text-anchor="middle" fill="var(--accent-color)" font-size="24" font-weight="700" letter-spacing="3">GD</text>
                </svg>
            </div>
            <div class="section-label">A DIFFERENT POINT OF VIEW</div>
            <h1>Grateful<br/><span>Dead.</span></h1>
            <p>Your world. Your perspective.</p>
            {#if clientInfo}
                <div class="build-info">
                    <span><small>MINECRAFT</small>{clientInfo.gameVersion}</span>
                    <span><small>CLIENT</small>{clientInfo.clientVersion}</span>
                    <span><small>CHANNEL</small>{clientInfo.development ? "Development" : "Release"}</span>
                </div>
            {/if}
        </aside>

        <div class="additional-buttons" transition:fly|global={{duration: menuDuration, y: 16, easing: menuEase}}>
            <ButtonContainer>
                <IconTextButton icon="icon-exit.svg" title="Exit" on:click={exitClient}/>
                <IconTextButton icon="icon-eye.svg" title="Basic Mode" on:click={toggleBasicMode}/>
                <IconButton icon="change-background" title="Toggle Shader" on:click={toggleBackgroundShaderEnabled}/>
            </ButtonContainer>
        </div>


    </div>
</div>

<style>
    .title-screen { position: relative; isolation: isolate; display: flex; flex: 1; min-height: 0; flex-direction: column; }
    .content { flex: 1; display: grid; grid-template-areas: "a d" "b b";
        grid-template-rows: 1fr max-content; grid-template-columns: minmax(480px, 650px) 1fr;
        gap: 48px 80px; }
    .navigation { grid-area: a; align-self: center; }
    .section-label { color: var(--menu-text-color); opacity: .45; font-size: 12px; letter-spacing: 3px; font-weight: 600; margin-bottom: 24px; }
    .main-buttons { display: flex; flex-direction: column; gap: 14px; min-height: 470px; }
    .identity { grid-area: d; position: relative; align-self: center; padding: 30px 0 40px 40px; }
    .identity .section-label { color: var(--accent-color); opacity: .85; }
    h1 { font-size: clamp(68px, 6vw, 180px); line-height: .96; letter-spacing: -.065em; font-weight: 750; position: relative; }
    h1 span { color: var(--accent-color); }
    .identity p { opacity: .5; font-size: 18px; margin-top: 24px; letter-spacing: -.3px; }
    .build-info { display: flex; gap: 40px; margin-top: 48px; font-size: 16px; }
    .build-info span { display: flex; flex-direction: column; gap: 9px; }
    .build-info small { opacity: .4; font-size: 10px; letter-spacing: 2px; }
    .cartography { position: absolute; right: 0; top: 50%; width: min(58vw, 720px); aspect-ratio: 1;
        transform: translateY(-50%); pointer-events: none; z-index: -1; }
    .cartography svg { width: 100%; height: 100%; }
    .orbit-ring { transform-origin: 360px 360px; animation: drift 50s linear infinite; }
    @keyframes drift { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) { .orbit-ring { animation: none; } }
    .additional-buttons { grid-area: b; align-self: end; }
    @media (max-width: 1400px) { .content { grid-template-columns: minmax(420px, 540px) 1fr; gap: 36px; } .identity { padding-left: 0; } }
    @media (max-width: 900px) { .content { grid-template-columns: 1fr; grid-template-areas: "a" "b"; } .identity { display: none; } }
</style>
