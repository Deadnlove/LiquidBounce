<script lang="ts">
    export let vertical = false;
</script>

<div class="button-container" class:vertical>
    <slot />
</div>

<style lang="scss">

    .button-container {
      border: 1px solid rgba(255, 255, 255, .08);
      background-color: var(--menu-button-container-background-color);
      padding: 12px 18px;
      width: max-content;
      border-radius: var(--menu-radius, 18px);
      display: flex;
      column-gap: 12px;

      &.vertical {
        flex-direction: column;
        row-gap: 20px;
        padding: 30px 15px;
      }
    }
</style>
