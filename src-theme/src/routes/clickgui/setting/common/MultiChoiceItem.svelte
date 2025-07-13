<script lang="ts">
    const { onclick, active, error, content }: { onclick: () => void, active: boolean, error: boolean, content: string } = $props();
</script>

<button class:active class:error {onclick}>
    {content}
</button>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  button {
    all: unset;
    color: $clickgui-text-dimmed-color;
    background-color: rgba($clickgui-base-color, 0.3);
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease color 0.2s;
    overflow-wrap: anywhere;

    &:hover {
      color: $clickgui-text-color;
    }

    &.error {
      background-color: rgba($menu-error-color, 0.1) !important;
      color: $menu-error-color !important;
    }

    &.active {
      background-color: rgba($accent-color, 0.1);
      color: $accent-color;
    }
  }
</style>
