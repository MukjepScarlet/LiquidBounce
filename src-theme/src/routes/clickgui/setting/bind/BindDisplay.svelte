<script lang="ts">
    import type {BindModifier} from "../../../../integration/types";

    export let modifiers: BindModifier[] | Set<BindModifier>;
    export let boundKey: string | undefined;

    $: parts = [...modifiers, boundKey]
        .filter(Boolean)
        .flatMap((item, index, array) =>
            index < array.length - 1
                ? [item, "+"]
                : [item]
        );
</script>

<span class="wrapper">
    {#if boundKey}
        {#each parts as part}
            <span class:divider={part === "+"}>{part}</span>
        {/each}
    {:else}
        <span class="dimmed">None</span>
    {/if}
</span>

<style lang="scss">
  @use "../../../../colors" as *;


  .wrapper {
    column-gap: 2px;
    display: flex;
    align-items: center;
  }

  .dimmed {
    color: $clickgui-text-dimmed-color;
  }

  .divider {
    color: $clickgui-text-dimmed-color;
    opacity: 0.8;
    font-size: 10px;
    line-height: 1;
    font-family: monospace;
  }
</style>
