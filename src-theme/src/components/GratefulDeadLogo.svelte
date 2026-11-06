<script lang="ts">
    export let width = "261.263px";
    export let height = "98px";
    export let badgeFill = "var(--accent-color)";
    export let badgeTextFill = "var(--text-color)";
    export let badgeGroupClass = "";
</script>

<svg class="grateful-dead-logo" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 261.263 98"
     role="img" aria-label="Grateful Dead" style={`width: ${width}; height: ${height};`}>
    <defs><slot name="defs" /></defs>
    <text x="0" y="48" fill="#fff" font-family="Inter, Arial, sans-serif" font-size="41"
          font-weight="750" letter-spacing="0.8" textLength="260" lengthAdjust="spacingAndGlyphs">GRATEFUL</text>
    <g class={badgeGroupClass}>
        <rect x="116" y="61" width="145" height="35" rx="10" fill={badgeFill}/>
        <text x="189" y="86" text-anchor="middle" fill={badgeTextFill} font-family="Inter, Arial, sans-serif"
              font-size="24" font-weight="750" letter-spacing="4">DEAD</text>
    </g>
</svg>

<style>
    .grateful-dead-logo {
        display: block;
        flex: 0 0 auto;
        overflow: visible;
    }
</style>
